# Design an AR Virtual Furniture Placement System — The Story (narrative edition)

> **What this file is.** The reference file, `54-Design-an-AR-Virtual-Furniture-Placement-System-FAANG-Guide.md`, is the one to recite from — requirements, API shapes, every trade-off table, the master cheat sheet. This file is a second way in: the same material as one continuous story, told in plain language. Engineers at a fictional furniture retailer, **Birchwood Home**, keep hitting a wall, patch it, and the patch itself creates the next wall — until we land on the exact same design the reference file documents. Birchwood is made up. But every wall it hits, and every fix it reaches for, leans on something real and named: Apple's **ARKit** and Google's **ARCore** (both real, documented on-device plane-detection/camera-tracking frameworks, shipping since 2017), **IKEA Place** (a real, documented consumer AR app that does exactly this kind of placement), the level-of-detail (LOD) technique game engines like Unreal and Unity have used for decades to manage 3D asset cost, and the same CDN/adaptive-streaming instincts behind video delivery. I'll flag with `[illustrative]` any time a specific number is a reasonable stand-in rather than a documented fact.

**The one sentence to hold onto:** an AR furniture app is really three different problems wearing one UI — a client-only rendering/tracking problem the backend can't touch, a 3D-asset-delivery problem that's basically adaptive-bitrate video applied to meshes instead of frames, and a tiny state-persistence problem that only looks big because the *room* it describes looks big. Everything below is just this idea getting harder, one honest step at a time.

---

## Chapter 1 — The couch that takes 17 seconds to show up

It's Birchwood Home's first crack at "See It At Home," their AR try-before-you-buy feature, built the fastest way anyone could think of: catalog page shows a "View in your room" button, and tapping it downloads the item's **full-detail 3D model** — high-poly mesh plus 4K textures, ~25MB for an average sofa `[illustrative, within the 15-40MB range real AR asset pipelines report for this level of detail]` — and only *after* that finishes does the AR camera view even open.

Real number: a typical US LTE connection runs somewhere around 10-15 Mbps down (documented in mobile speed-test aggregates like Ookla's), call it ~1.5MB/sec. A 25MB model at that speed takes **~17 seconds** — and for those 17 seconds the user is staring at a spinner, not their living room. Early analytics on the pilot: **42% of sessions abandon** before the model even finishes loading `[illustrative]`. Nobody bought a couch; they just closed the app.

```mermaid
sequenceDiagram
    participant U as User
    participant App as Birchwood App
    participant CDN as Asset store

    U->>App: tap "View in your room"
    App->>CDN: download FULL model (25MB)
    Note over App,CDN: 17 seconds, nothing shown yet
    CDN-->>App: model finally arrives
    App->>App: open camera, start AR tracking
    App->>U: sofa finally appears
    Note over U: many users already gave up
```

The obvious question: *why does the user need the full-detail, ready-to-manufacture-looking model just to check whether a couch roughly fits in the corner?* They don't — that level of fidelity only matters once they're actually looking at it in their space, deciding. Browsing and deciding-to-place are two different jobs with two different quality bars.

**The fix, and the analogy for the rest of this story:** split every item into **two tiers** — a tiny **preview** model for browsing, and the big **full-detail** model, fetched only when the user actually commits to placing that specific item. Think of it like a furniture showroom's own paper catalog: you flip through *photos* (preview) to decide what you like, and only once you've picked one do you request the *actual floor sample* (full detail) to go stand next to. Nobody ships you the real couch just so you can flip past its page.

**New problem, immediately:** browsing is fast and cheap now — previews are ~300KB, essentially instant. But tapping "place" still triggers the same 17-second full-detail download as before, just later in the flow. The blank wait didn't go away, it moved.

**How I'd say this in an interview:** "The naive version fetches full-detail geometry for every interaction, browsing included, and that's 40-80x more data than browsing actually needs. The fix is a two-tier asset model — cheap previews for browsing, full detail only on commit — but that alone doesn't fix the placement moment itself, which is the very next problem."

---

## Chapter 2 — The floor sample you can show *right now*

Splitting into tiers fixed browsing. Placement is still broken: the user taps "place," and for ~17 seconds there's an empty camera feed with nothing rendered — reads as *broken*, not *loading*.

The obvious next question: *does the user really need to see nothing at all for those 17 seconds?* No — they already have something useful sitting in memory: the small preview model they were just browsing with a second ago, still cached on the phone.

**The fix:** show that cached preview model **immediately**, full-size, anchored at the tapped spot, while the real full-detail mesh streams in behind it. Extending the showroom analogy: it's the floor sample again — you get to see a stand-in couch, the right size and shape, in your actual room *right now*, while the fabric-and-frame you specifically ordered gets built and delivered in the background. When it's ready, swap it in — the same "show something plausible now, sharpen it progressively" idea behind progressive JPEG loading or a blurred placeholder in a photo feed, just applied to 3D geometry instead of pixels.

```mermaid
flowchart LR
    A["Tap 'place'"] --> B["Show cached preview\nmodel INSTANTLY\n(showroom floor sample)"]
    B --> C["Stream full-detail mesh\n+ textures progressively"]
    C --> D["Swap preview -> full detail\nonce it finishes,\nno visible gap"]
```

Worked result: perceived wait for *something* to appear drops from 17 seconds to **well under a second** — the preview was already cached from browsing a moment earlier. The full 25MB swap-in still takes roughly the same ~17 seconds on that connection, but it now happens invisibly, in the background, while the user is already looking at a reasonable stand-in and deciding if the shape/scale works.

**New problem:** this assumes the full-detail stream *eventually* finishes. What happens if the connection is bad enough that it never does?

**How I'd say this in an interview:** "The fix isn't making the full download faster — it's never letting the user stare at nothing while it happens. Show the already-cached preview instantly as a placeholder, stream full detail in behind it, swap when ready. It's the same progressive-loading instinct as a blurred photo placeholder, just for meshes."

---

## Chapter 3 — The download that never finishes

Birchwood pilots "See It At Home" in a few physical stores too — customers standing in the showroom, phone in hand, on the store's guest wifi. That wifi, on a bad day, gives out roughly **200 Kbps** `[illustrative — a stand-in for "genuinely bad in-store wifi," not a measured figure]`. At 200 Kbps, that same 25MB full-detail model takes **over 15 minutes**. Nobody's holding their phone up for 15 minutes. The stream just... never completes, and the app has no plan for that.

The obvious question: *what should happen when full detail simply isn't going to arrive in any reasonable time?*

**The fix:** put a cap on the wait. If the full-detail stream hasn't completed within a few seconds, stop waiting — stay on the showroom-floor-sample placeholder, but add a **visible tag** so the user knows more detail is still coming ("upgrading quality..."), and keep streaming quietly in the background in case it eventually catches up. Never an indefinite spinner, never a silent stall — either it loads, or the user is clearly told they're looking at the stand-in and why.

```mermaid
sequenceDiagram
    participant U as User
    participant App as Birchwood App
    participant CDN as Asset store

    U->>App: tap "place" (in-store, bad wifi)
    App->>App: show preview placeholder instantly
    App->>CDN: stream full-detail asset
    Note over App,CDN: 200 Kbps -- stream stalls past a reasonable wait
    App->>App: give up waiting, keep the placeholder, add "upgrading quality" tag
    App->>U: sofa shown at reduced detail, clearly labeled, never blank
    Note over App,CDN: full stream keeps going quietly; swaps in if it ever finishes
```

**New problem:** the per-item loading experience is solid now. But zoom out from "one user, one item" to the whole business: Birchwood's catalog is growing toward **20,000 SKUs**, and hundreds of thousands of sessions a day are hitting it. Is fetching *any* of this, at that scale, actually affordable and fast for everyone, everywhere?

**How I'd say this in an interview:** "A stalled stream is a UX decision, not a networking one — cap the wait, fall back to the placeholder you already have, tag it visibly, and keep trying in the background. The user should never be left guessing whether the app is broken or just slow."

---

## Chapter 4 — The regional warehouse trick

Birchwood's catalog has grown to **20,000 SKUs**, and "See It At Home" now sees **500,000 sessions a day**. Worked estimate, same shape the reference guide walks through: each session previews roughly **15 items** while browsing (~350KB average preview each) and places roughly **3 items** in AR (~25MB average full-detail each).

```
Preview bandwidth  = 500,000 sessions x 15 items x ~350KB  ~= 2.6 TB/day
Full-detail bandwidth = 500,000 sessions x 3 items x ~25MB  ~= 37.5 TB/day
```

The full-detail tier moves **~14x more data** than the preview tier despite covering **5x fewer items per session** — because a full-detail asset is 40-80x bigger than a preview one. That gap is the whole reason the two-tier split from Chapter 1 exists at all; this is just the bill for skipping it.

```mermaid
pie showData
    title Daily asset bandwidth: preview vs. full-detail tier
    "Full-detail (placement)" : 37500
    "Preview (browsing)" : 2600
```

The obvious question: *does Birchwood need to run beefy servers, everywhere in the world, to serve 40 TB/day of the same 20,000 SKUs' worth of files?* No — and here's the giveaway: **every single user is requesting the same shared catalog.** This isn't a personalized feed where every response is unique; it's the same "sofa_4471, full-detail, v3" file being requested over and over by different people. That's the single most cacheable shape a workload can have.

**The fix:** a CDN, populated from one **origin store** that holds the master, versioned copy of every SKU at every LOD tier. Think of it as a **regional warehouse network**: one central factory (the origin store) makes each item once; regional warehouses (CDN edge nodes) keep the popular ones stocked close to shoppers, so almost every request is served locally, fast, without a slow trip back to the factory. The factory only gets bothered on a genuine stock-out — a cache miss.

```mermaid
graph LR
    Origin[("Origin store:\nversioned model files\nper SKU, per LOD tier\n(the factory)")]
    Origin -.->|"populate on\ncache miss"| Edge1[("CDN edge -\nregion A")]
    Origin -.-> Edge2[("CDN edge -\nregion B")]
    Edge1 -->|"cache hit,\nfast"| U1[Users nearby]
    Edge2 -->|"cache hit,\nfast"| U2[Users nearby]
```

**New problem:** the asset pipeline is now fast, affordable, and scaled. But it only covers *getting a model to the phone*. It says nothing about what happens after: a user spends 20 minutes placing 5 items, gets called away for dinner, closes the app — and comes back the next day. Where did that arrangement go?

**How I'd say this in an interview:** "At this scale it's a standard CDN problem, not a novel one — the catalog is shared, heavily cacheable content, so you scale by adding edge capacity, exactly like any video or image CDN. The interesting number is that full-detail assets dominate total bandwidth despite being fetched far less often, purely because of the size gap — that's the number that justifies the whole two-tier design."

---

## Chapter 5 — Saving a photo of the room vs. saving the moving company's sheet

A user places a sofa, two side tables, and a rug, closes the app, and comes back tomorrow expecting it all still there. The first instinct on the team: *"just save what the room looked like"* — capture a screenshot, or export the whole rendered 3D scene as a file, and replay that on reopen.

**Why that's the wrong instinct, worked out:** a rendered export or point-cloud snapshot of a 5-item scene could easily run several megabytes `[illustrative]`, dwarfing anything else in this design. Worse, it goes **stale** the instant Birchwood fixes a texture bug on one of those SKUs — the saved snapshot never picks that up, because it's a frozen picture, not a live reference. And it's **not editable** — nudging one side table ten centimeters means re-capturing the entire room, not adjusting one number.

The obvious question: *what's the smallest thing that fully describes "which item, where" without describing what it looks like?*

**The fix:** don't save a photo of the room — save the **moving company's placement sheet**. A list of *which piece goes where*: item ID, position, rotation, scale. Nothing about geometry, nothing about textures — those live once, in the catalog, and get re-fetched (often still cached) whenever the sheet is replayed.

```mermaid
flowchart LR
    A["Naive: save a rendered\nsnapshot / point cloud\nof the room"] -->|"large, stale,\nnot editable"| X["Rejected"]
    B["Fix: save the\nplacement sheet --\nitemId + position +\nrotation + scale, per item"] --> C["Reload = re-fetch each\nitem's CURRENT asset\n(cached, likely) + replay\nthe sheet"]
```

Worked number: a scene with 8 placed items, at roughly 80 bytes per item (id + position + rotation + scale), comes to **~640 bytes total** — smaller than this paragraph, regardless of how visually rich the room looks, because the actual 3D data lives once in the shared catalog, not once per saved room.

**New problem:** this placement sheet references items by SKU ID. Furniture retailers discontinue and redesign SKUs constantly. What happens when a sheet points at a couch Birchwood stopped selling eight months ago?

**How I'd say this in an interview:** "A scene should be metadata — a small graph of item references and transforms — never rendered output. That's what makes save and reload cheap, fast, editable, and automatically up to date with whatever the catalog asset currently looks like, instead of frozen at save time."

---

## Chapter 6 — The placement sheet that outlives the couch

Six months after a customer saves a room with `sku_4471` (a particular sofa) placed in it, Birchwood discontinues that exact model as part of a seasonal refresh — a completely normal, real retail lifecycle event. The customer reopens their saved room. Their placement sheet still says `sku_4471`, position (1.2, 0, 0.8). But that SKU's asset is gone from the live catalog.

The obvious question: *does the app crash, show a hole in the room, or something else?*

**The fix:** two things, layered onto the reference-based model from Chapter 5. First, assets are **versioned** per SKU, per LOD tier — so "discontinued" doesn't have to mean "deleted"; the last-known version can still be served to old scenes even after it's pulled from active browsing. Second, give a **scene** (and its items) an explicit lifecycle, so "this reference is stale" is a defined state, not an unhandled crash:

```mermaid
stateDiagram-v2
    [*] --> Draft: user starts placing (local, unsaved)
    Draft --> Saved: user taps "save room"
    Saved --> Draft: user reopens to edit
    Saved --> Archived: a referenced item is discontinued
    Archived --> [*]
```

```mermaid
erDiagram
    SCENE ||--o{ PLACED_ITEM : contains
    CATALOG_ITEM ||--o{ PLACED_ITEM : "referenced by"
    CATALOG_ITEM ||--o{ ASSET_VARIANT : has

    SCENE {
        string sceneId PK
        string ownerUserId
        string status
    }
    PLACED_ITEM {
        string sceneId FK
        string itemId FK
        float posX
        float posY
        float posZ
        float rotY
        float scale
    }
    ASSET_VARIANT {
        string itemId FK
        string lodTier
        string versionId
        string cdnUrl
    }
```

With this in place, reopening the sheet for `sku_4471` shows the last-known cached version with a small "discontinued" tag, instead of a hole in the floor or a crash — a deliberate product decision, not an accident.

**New problem:** none from lifecycle handling itself — but a completely different request lands on the team: a couple furnishing an apartment together wants to see each other's changes to the *same* room, live, at the same time.

**How I'd say this in an interview:** "Reference-based persistence buys you editability and freshness, but it comes with a real decision to make explicitly: define what happens when a reference goes stale. `Archived` with a defined fallback — never an unhandled missing reference — is the concrete answer."

---

## Chapter 7 — Sticky notes on a shared placement sheet

Two people, same account-linked session, both looking at the same room: one moves the sofa, the other should see it move too, quickly. Birchwood's first pass reuses the *existing* save pipeline: every nudge triggers a full `POST` of the whole scene, then the other device does a full `GET` to reload it. Worked number from the first internal build: a single couch-nudge took **~800ms** to show up on the other phone `[illustrative]` — not terrible, but visibly laggy for something that should feel instant, and wasteful: re-sending and re-parsing the *entire* 8-item sheet just to move one item 10cm.

The obvious question: *does moving one couch really need to go through the whole "save the entire room" pipeline every time?*

**The fix:** a lightweight realtime layer that broadcasts just the **one small transform delta** that changed — not the whole scene, and critically, **not any asset data**, because both phones already have that item's asset cached from browsing a moment ago. Sticking with the placement-sheet analogy: you don't reprint and redistribute the whole sheet every time someone moves a sticky note on it — you just say "sofa moved to here" out loud, and everyone updates their own copy of the sheet.

```mermaid
sequenceDiagram
    participant A as User A
    participant Collab as Collaboration server
    participant B as User B

    A->>Collab: move sofa to (1.5, 0, 0.9)
    Collab->>Collab: apply to shared scene state
    Collab->>B: broadcast: sofa moved to (1.5, 0, 0.9)
    B->>B: render sofa's already-cached asset at new position
    Note over A,B: only a ~30-byte transform moved -- no asset data crossed the wire
```

**Why this is a much easier sync problem than character-by-character text editing:** a transform update (position/rotation/scale) is naturally last-writer-wins *at the object level* — two people grabbing the *exact same* couch at the *exact same* instant is rare and low-stakes; whichever update lands last wins, and it visually settles in well under a second. That's a much lighter bar than true text-collaboration CRDTs, which have to reconcile interleaved edits to the *same characters*.

**New problem:** none introduced by collaboration itself — but it raises an obvious question from the rest of the team: given how much machinery this design already has, why not just render everything on a beefy cloud GPU and stream the video down, so the phone never needs any LOD tiers or streaming at all?

**How I'd say this in an interview:** "Broadcast the delta, not the scene, and definitely not the asset — every client already has the asset cached. Last-writer-wins per object is fine here specifically because same-object collisions are rare and low-stakes, unlike text editing where two people can genuinely be typing into the same sentence."

---

## Chapter 8 — The friend on the phone who can't catch your fall

Someone on the team proposes it seriously: render the sofa on a powerful cloud GPU, always full detail, and stream the AR view down as video — the same idea behind cloud game streaming. It would mean no LOD tiers, no progressive loading, no placeholder swap — the cloud always has full detail ready.

They prototype it. Real number: round-tripping a camera frame to the cloud, rendering, encoding, and streaming a frame back adds roughly **150-250ms** on top of render time on a decent connection `[illustrative — network RTT + encode/decode overhead, not a measured Birchwood figure]`, versus a few milliseconds for rendering directly on the phone's own GPU.

Here's why that number kills the idea outright, not just makes it "a bit slower": AR isn't a normal video stream where a little lag is tolerable. The rendered sofa has to track the **camera's live movement**, frame by frame, to look glued to the floor as the user walks around it. Any gap between "phone moves" and "rendered image catches up" shows up immediately as the sofa swimming or lagging behind — the whole illusion of "it's really there" collapses.

**The analogy:** imagine trying to balance on one foot, but instead of your own inner ear telling you *instantly* that you're tipping, you have to describe your position to a friend on a phone call and wait for them to say "lean left." By the time you hear it, you've already fallen. Camera tracking and rendering have to happen as fast and as locally as your own sense of balance — you can't outsource that over a network round trip, no matter how good the connection is.

```mermaid
quadrantChart
    title Where should rendering happen?
    x-axis Low latency --> High latency
    y-axis Works for live camera tracking --> Doesn't
    quadrant-1 Fine for non-AR preview
    quadrant-2 The right call for AR
    quadrant-3 Wrong for anything
    quadrant-4 Breaks AR's tracking loop
    On-device (client): [0.15, 0.15]
    Cloud-rendered video: [0.75, 0.8]
```

**The actual conclusion:** client-side rendering isn't really a *choice* for AR specifically — the camera-to-render latency budget doesn't tolerate a network hop. Cloud rendering is a perfectly legitimate answer for a **different, non-AR** feature Birchwood also has — a 360° "spin the couch" product viewer with no live camera involved — because there's no tracking loop there to protect. Say that distinction out loud; it shows you're not rejecting cloud rendering wholesale, just correctly scoping where its latency cost is and isn't acceptable.

**New problem:** given the phone *must* render and track locally, the backend has genuinely zero lever over one very visible failure mode: what happens when the phone's own tracking gets confused?

**How I'd say this in an interview:** "AR rendering is client-side by near-necessity, not by preference — the render has to track the camera in real time, and a network round trip breaks that illusion immediately, the same way a delayed balance signal would make you fall. Server-side rendering is a fine answer for a non-AR 3D preview with no camera tracking loop, but not for anything anchored to a live feed."

---

## Chapter 9 — When the phone loses its footing

A user tries "See It At Home" in a dimly lit bedroom, moving the phone quickly to check the sofa from different angles. ARKit and ARCore — real, documented on-device frameworks — do plane detection and camera tracking using the phone's camera plus its inertial sensors, and both frameworks document that poor lighting, low-texture or reflective surfaces, and fast motion all degrade that tracking. In this session, tracking briefly loses its lock, and the placed sofa visibly **drifts a few centimeters and clips through the floor** for a couple of seconds `[illustrative — a concrete stand-in for a real, well-known class of AR-tracking failure]` before re-anchoring.

The obvious question, and the one interviewers like to ask specifically to see if a candidate overreaches: *what does the backend do about this?*

**The honest answer: basically nothing, and that's correct, not a gap.** Plane detection and camera tracking are entirely a client/platform-framework concern — there is no backend service, queue, or database that improves it. Saying this plainly, without trying to sketch a fix for it, is itself the right move in an interview; it's the same "AR tracking is the phone's problem" boundary drawn all the way back at the start of this story.

```mermaid
flowchart TD
    A["Tracking drifts/re-anchors\n(poor light, fast motion)"] --> B{"Is this a backend\ndesign problem?"}
    B -->|"No -- client/platform\nframework concern"| C["Say so explicitly.\nNo backend lever here."]
    B -.->|"one adjacent thing\nbackend DOES control"| D["Keep assets already\ncached client-side, so\nre-anchoring doesn't ALSO\ntrigger a fresh asset fetch"]
```

The one adjacent thing the backend design *does* still control: making sure re-anchoring doesn't compound into a second problem, by keeping the item's asset already cached client-side (per the CDN work in Chapter 4) — so recovering from a tracking hiccup is instant, not also a fresh multi-second download.

**Worth naming alongside this — the privacy angle:** the raw camera frames used for tracking are processed entirely on-device and are never uploaded or stored server-side. The backend only ever sees the *result* — position/rotation transforms — never a video of someone's living room. That's a meaningfully smaller and safer privacy surface than it might sound like at first, and it's worth stating explicitly rather than leaving it implied.

**New problem:** tracking and rendering are settled. One more everyday scenario remains — not about multiple *people* this time, but the same person using multiple *devices*.

**How I'd say this in an interview:** "This one has no backend fix, and saying that clearly is the correct answer, not a dodge — plane detection and tracking are a client-framework capability I'm treating as a given. The one thing I'd still design for is making sure re-anchoring doesn't also trigger a fresh asset download, by keeping things cached."

---

## Chapter 10 — The tablet that overwrites the phone

A customer starts a room layout on their phone at the store, then continues at home on a tablet, same account. Both devices had the scene open before either one saved. Sequence: phone edits the sofa's position and saves at t=0s. The tablet, still holding the *old* state from before that edit, gets touched 45 seconds later and saves too — silently overwriting the phone's more recent change. The sofa snaps back to where it was. The customer has no idea why.

The obvious question: *does this need the same real-time collaboration machinery from Chapter 7 — broadcasting live transform deltas between the phone and tablet?*

**No — and it's worth explaining why not, because it's a genuinely different shape of problem.** Chapter 7's collaboration server exists because *two different people* are actively editing *together*, live, and need to see each other's changes propagate. This is *one person's own two devices*, not editing at the same instant, just occasionally out of sync — a much lower-collision case that doesn't justify standing up a live sync layer.

**The fix:** simple **optimistic concurrency** — each saved scene carries a version number (or `updatedAt`). A save only succeeds if the device's local version matches what the server currently has; if the tablet tries to save against a version the phone has already moved past, the save is rejected (or flagged) instead of silently overwriting.

```mermaid
sequenceDiagram
    participant Phone
    participant Server
    participant Tablet

    Phone->>Server: save scene (based on v1)
    Server-->>Phone: saved as v2
    Note over Tablet: still holds v1 locally
    Tablet->>Server: save scene (based on v1)
    Server-->>Tablet: REJECTED -- server is already at v2, refresh first
    Note over Tablet: no silent overwrite -- tablet re-fetches v2, then edits on top of it
```

This closes the loop for single-owner scenes without borrowing any of the heavier real-time infrastructure that genuinely-collaborative editing needs.

**How I'd say this in an interview:** "This looks like the same problem as multi-user collaboration but it isn't — one owner across two devices is low-collision, so a cheap version check on save is enough. Reaching for full real-time sync machinery here would be solving a problem that doesn't actually exist yet."

---

## Where the story actually lands

```mermaid
flowchart LR
    A["Ch1: full-detail\nfor every browse\n(17s blank wait)"] -->|"fixes: fast browsing\nbreaks: placement still blank"| B["Ch2: cached preview\nas instant placeholder"]
    B -->|"fixes: instant feel\nbreaks: bad connections stall forever"| C["Ch3: timeout +\nvisible fallback"]
    C -->|"fixes: per-item UX\nbreaks: is this affordable at scale?"| D["Ch4: CDN,\nregional-warehouse model"]
    D -->|"fixes: bandwidth/scale\nbreaks: where does a layout live between sessions?"| E["Ch5: scene = placement\nsheet, not a snapshot"]
    E -->|"fixes: cheap, editable\nbreaks: SKU gets discontinued"| F["Ch6: versioned assets\n+ Archived lifecycle"]
    F -->|"fixes: safe stale refs\nbreaks: two people, one room"| G["Ch7: broadcast deltas,\nlast-writer-wins"]
    G -->|"fixes: live collab\nraises: why not render in the cloud?"| H["Ch8: client-side\nrendering, near-mandatory"]
    H -->|"raises: what about\ntracking failures?"| I["Ch9: tracking is the\nclient's problem, full stop"]
    I -->|"raises: same user,\ntwo devices"| J["Ch10: optimistic\nconcurrency on save"]
```

```mermaid
mindmap
  root((Why an AR furniture app\nneeds all of this))
    Asset delivery
      full detail for browsing = 40-80x too much
      two-tier assets + progressive streaming
      CDN = regional warehouses for a shared catalog
      graceful fallback, never a blank stare
    Scene persistence
      a room is a placement sheet, not a photo
      tiny -- ~80 bytes per item
      stale references need a defined lifecycle
    Collaboration
      broadcast deltas, not assets
      last-writer-wins per object is fine here
      same-device conflicts are a smaller problem: version check
    Rendering & tracking
      AR needs camera-to-render latency near zero
      client-side is near-mandatory, not a preference
      tracking failures: client-only, no backend lever
      privacy: only transforms leave the device, never camera frames
```

The skill isn't reciting all ten chapters — it's knowing where the stated requirements say to stop. A single-user MVP reasonably stops around Chapter 6. The moment "share with someone else" comes up, you're in Chapter 7. If nobody's asked about multi-device conflicts, walking there unprompted reads as padding, not depth.

---

## Grill me — adversarial follow-ups

**Q1: "Why not just always show the full-detail model — why bother with a preview tier at all?"**
Because the size gap is the whole problem: a full-detail asset is 40-80x bigger than a preview, and most of a session is spent browsing, not placing. Fetching full detail for every browsed item means paying that 40-80x cost for items the user never actually places — the two-tier split exists purely because browsing and deciding-to-place have different fidelity needs.

**Q2: "Isn't the mid-LOD placeholder just a nicer-looking loading spinner? What's actually different?"**
A generic spinner tells the user "wait, unknown duration, unknown outcome." A placeholder at the item's real size and position, in the right spot, tells them "this is roughly what you're getting, and it's about to get sharper" — same UX principle as a blurred image placeholder, and it lets the user start evaluating fit and scale immediately instead of waiting for full resolution to make any judgment at all.

**Q3: "Why is a scene stored as a graph of references instead of just caching the last-rendered frame?"**
Because a cached frame is frozen — it doesn't stay in sync if the catalog updates that item's texture, it can't be nudged without re-rendering everything, and it's enormously larger than a list of positions. References plus transforms are small, editable, and automatically current with whatever the catalog's latest asset version looks like.

**Q4: "Given the collaboration chapter, why not just always use the same real-time layer for the multi-device-same-user case too?"**
Because it's solving a different problem at a different collision rate — two people editing together live is genuinely simultaneous and needs propagation; one person's phone and tablet being out of sync by 45 seconds is a rare, low-stakes conflict that a cheap version check resolves without standing up any live infrastructure.

**Q5: "You said AR rendering has to be client-side — isn't that true of any real-time app, so why call it out specially here?"**
Most real-time apps tolerate some latency — a chat message landing 200ms late is barely noticeable. AR specifically renders on top of a live camera feed that the user's own eyes are also watching in real time, so any lag between camera motion and render update is instantly visible as the object swimming or lagging — there's no comparable tolerance window to hide behind.

**Q6: "What actually breaks if you skip the CDN and just serve assets from one origin server?"**
At 500,000 sessions a day pulling from a shared 20,000-SKU catalog, that's tens of terabytes a day hitting one machine's network and disk — it becomes the throughput ceiling and, being one machine, a single point of failure too. A CDN is the standard fix precisely because this catalog is the exact shape of workload CDNs are built for: the same content, requested repeatedly, by everyone.

**Q7: "If tracking failures are entirely a client concern, why mention them in a backend system design interview at all?"**
Because correctly scoping something as out-of-scope is itself a signal — interviewers sometimes probe this exact spot to see if a candidate tries to redesign SLAM or plane detection instead of stating the boundary and moving on. The one legitimate backend-adjacent point is making sure re-anchoring doesn't also trigger a fresh asset fetch, since that part actually is under your control.

**Q8: "What's the actual cost trade-off of maintaining multiple LOD tiers per SKU?"**
You're trading art-production and pipeline complexity — someone has to generate and maintain a low-poly preview and a high-poly full-detail version of every SKU — for a bandwidth saving on the order of 40-80x on the tier that dominates daily traffic. At any catalog size beyond a handful of items, that trade is easy to justify.

**Q9: "How would you handle a user placing 30 items in one room instead of 5 — does anything in this design change?"**
Almost nothing changes on the backend — a 30-item scene is still on the order of a couple kilobytes of transforms, trivial to store and transmit. The only place it shows up is the client needing to manage more concurrently cached assets and more simultaneous AR-anchored objects, which is a rendering-performance concern for the client platform, not a scene-storage concern for the backend.

**Q10: "If you had to cut this whole design down to the one thing that matters most, what is it?"**
Decompose first, out loud, before designing anything: AR tracking and rendering are a client-platform capability, not something you're redesigning; asset delivery and scene persistence are the actual backend problems, and that's where the interview time should go. Getting that split right in the first minute is worth more than any individual deep dive that follows it.

---

## Cheat sheet — one line per stop on the story

- **Full-detail-for-everything**: 40-80x more data than browsing needs — the reason a two-tier asset model exists at all.
- **Two-tier assets**: small preview for browsing, big full-detail only fetched on commit — like a catalog photo vs. the actual floor sample.
- **Cached preview as instant placeholder**: show what you already have the instant the user commits, stream full detail in behind it, swap when ready — never a blank stare.
- **Timeout + visible fallback**: if full detail stalls past a reasonable wait, keep the placeholder, tag it clearly, keep streaming quietly in the background.
- **CDN, regional-warehouse model**: the catalog is shared, identical content for every user — extremely cacheable; scale by adding edge capacity, not origin capacity.
- **Scene = placement sheet, not a snapshot**: item references + position/rotation/scale, ~80 bytes per item — small, editable, and automatically current with the live catalog.
- **Versioned assets + Archived lifecycle**: a discontinued SKU still referenced by an old scene needs a defined fallback, never an unhandled missing reference.
- **Broadcast deltas, last-writer-wins**: real-time collaboration here only needs to move small transforms, not assets — same-object collisions are rare and low-stakes, unlike text-editing CRDTs.
- **Client-side rendering, near-mandatory for AR**: the camera-to-render latency loop has ~zero tolerance for a network round trip; cloud rendering is fine for non-AR previews with no tracking loop to protect.
- **Tracking failures are a client-only concern**: no backend lever beyond keeping assets already cached so re-anchoring doesn't also trigger a fresh download; camera frames never leave the device.
- **Optimistic concurrency for same-user multi-device**: a cheap version check on save is enough — don't reach for live-collaboration machinery for a low-collision, single-owner case.
- **The meta-lesson**: every fix here buys one property — browsing speed, placement responsiveness, bandwidth affordability, persistence cheapness, reference safety, collaboration liveliness, tracking fidelity, or conflict safety — by spending effort somewhere specific; say the trade in the same breath as the fix.
