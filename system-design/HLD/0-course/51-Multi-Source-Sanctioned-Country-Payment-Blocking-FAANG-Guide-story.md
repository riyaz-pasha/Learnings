# Design a Multi-Source Sanctioned-Country Payment Blocking System — The Story (narrative edition)

> **What this file is.** The reference file,
> `51-Multi-Source-Sanctioned-Country-Payment-Blocking-FAANG-Guide.md`, is the one to recite from —
> requirements, API shapes, the aggregation formula, the master cheat sheet. This file is a second
> way in: the same material as one continuous story, told in plain language. Engineers at a
> fictional cross-border payments processor, **Tradewind Payments**, keep hitting a wall, patch it,
> and the patch itself creates the next wall — until we land on the exact design the reference file
> documents. Tradewind itself is fictional. But every wall it hits is something real: the US
> Treasury's OFAC and its Specially Designated Nationals (SDN) list, the EU's own consolidated
> sanctions list, the UN Security Council Consolidated List, the well-documented fact that OFAC
> maintains a comprehensive embargo on Cuba that the EU does not mirror, the real 2019 escalation
> of US sanctions on Venezuela, and the largest sanctions-violation settlement in history — BNP
> Paribas's $8.9 billion fine in 2014. I'll say clearly, every time, whether a number is documented
> fact or a reasonable stand-in, tagged `[illustrative]`.

**The one-sentence core idea:** a cross-border payment is never sanctioned or cleared by *one*
authority's yes/no — it's governed by however many independent regulators actually have a legal
claim on it, and the hardest part of this whole system isn't checking a list, it's figuring out
*which* lists even apply to this specific payment before you can ask any of them anything.

---

## Chapter 1 — The list taped above the terminal

It's 2016. Tradewind is a small cross-border payments startup connecting merchants in Southeast
Asia and Latin America to buyers worldwide. An engineer, tasked with "add sanctions compliance"
in a sprint, reads the OFAC website once, and writes this:

```python
BLOCKED_COUNTRIES = ["IR", "KP", "SY", "CU"]  # Iran, North Korea, Syria, Cuba
```

This matches OFAC's traditional comprehensively-sanctioned countries at the time — a real,
documented list — and it ships. For two and a half years, it works fine. Nobody touches it again.

In January 2019, the US Treasury sharply escalates sanctions on Venezuela's government and its
state oil company PDVSA (Executive Order 13884, a real, documented action), moving Venezuela from
"some restrictions" to something close to a comprehensive government-level block. Tradewind's
hardcoded array still reads `["IR", "KP", "SY", "CU"]`. Venezuela was never on it, and nothing
about this new executive order changes a Python list living in a repo nobody's opened in years.

Worked number: by early 2019 Tradewind processes roughly **40,000 cross-border payments/day**
`[illustrative]`. Even a small slice touching Venezuelan payers or payees — say **0.4%**, about
**160 payments/day** `[illustrative]` — now keeps flowing straight through, fully approved, to a
jurisdiction the US government just placed under sweeping restrictions. It takes **11 days**
`[illustrative]` before a compliance analyst doing a routine sample review notices and escalates.

```mermaid
flowchart LR
    A["OFAC designates\nVenezuela, Jan 2019"] -.->|"nobody tells the array"| B["BLOCKED_COUNTRIES\n= [IR, KP, SY, CU]"]
    C["Venezuelan payment\narrives"] --> B
    B -->|"VE not in the list"| D["APPROVED\n(wrong)"]
```

The obvious question: *why does a list like this ever go eleven days without anyone knowing it's
wrong?* Because nothing in the system watches the list itself — it's just a constant, and constants
don't expire loudly. The real-world stakes for getting this wrong are not abstract: BNP Paribas
paid **$8.9 billion** in 2014 — still the largest sanctions-violation settlement ever — for
processing transactions tied to sanctioned countries including Sudan, Iran, and Cuba (a real,
widely documented case). Tradewind is nowhere near that scale, but the failure mode is identical:
a payment that should have been blocked, wasn't, because the list making that call was stale.

**The fix, and the name for it:** stop hardcoding the list. Pull it from the actual source — OFAC
publishes its SDN list as a downloadable, machine-readable file — on a schedule, automatically.
Call this **the photocopy**: instead of one engineer reading a webpage once and writing it into
code by hand, a job goes and photocopies OFAC's actual, current list every day and Tradewind's
checks run against *that*, not against anyone's memory of what the list used to say.

**New problem, immediately:** OFAC doesn't only publish on a nice fixed daily schedule — designations
can land the moment a real-world event happens, and a once-a-day photocopy job means the *worst*
case gap between "OFAC changed the list" and "Tradewind's copy reflects it" is a full day, not
zero.

**How I'd say this in an interview:** "The very first bug in this whole space is always the same
one: someone hardcodes a snapshot of a sanctions list at one point in time, and nothing re-checks
it. The fix is automating the pull from the actual authoritative source on a schedule — but that
immediately raises the next question, which is how fresh 'on a schedule' actually needs to be."

---

## Chapter 2 — The photocopy that's a day behind reality

Tradewind builds the photocopy job: pull OFAC's SDN file nightly, parse roughly **15,000 entries**
(the real, documented rough size of OFAC's SDN list), build a lookup index, swap it in. This is
mechanically identical to the ingestion pattern any single-source sanctions-list guide would
teach — pull, validate, version, swap.

```mermaid
sequenceDiagram
    participant OFAC as OFAC SDN feed
    participant Job as Nightly photocopy job
    participant Idx as Lookup index
    participant Txn as Payment check

    OFAC->>Job: full SDN file (~15,000 entries)
    Job->>Job: parse, validate, version it
    Job->>Idx: swap in new snapshot
    Txn->>Idx: is this payer/payee on the list?
    Idx-->>Txn: fast, local, no network call needed
```

Six months later, OFAC issues an out-of-cycle designation in response to a real-world event — this
is documented, normal OFAC behavior; urgent designations don't wait for a convenient publishing
day. Tradewind's job runs at 2 AM and won't run again for **24 hours**. A newly-designated entity's
payment sails through for up to that entire day, simply because the photocopy hadn't been retaken
yet. Worked number: at 40,000 payments/day, even a worst-case one-day staleness window on a single
freshly-designated entity is a real, live compliance gap, not a theoretical one — it's just smaller
and rarer than Chapter 1's eleven-day gap.

The obvious next question: *so just photocopy more often — every hour, every minute?* You can, and
Tradewind moves to hourly pulls, shrinking the worst case from 24 hours to roughly **1 hour**. But
notice this only shrinks the window — it doesn't remove it, and pulling more often doesn't fix the
actual problem coming next.

**How I'd say this in an interview:** "Automating the pull fixes 'nobody's watching the list' but
it doesn't make staleness zero — there's always a window between when the source changes and when
your copy reflects it. The right move is to make that window small, monitored, and disclosed — not
to pretend it's zero — and that's a fine place to stop if the interviewer's only asked about one
source. The next problem shows up the moment legal says one source isn't enough."

---

## Chapter 3 — Three embassies, three clipboards

Tradewind starts settling payments for EU-based merchants and, separately, starts clearing some
transactions in US dollars through a US correspondent bank. Legal flags two new obligations at
once: EU law requires screening against the **EU's own consolidated sanctions list**, and USD
clearing through the US financial system pulls OFAC's jurisdiction into transactions that have
nothing to do with American parties. Both are real, documented sources Tradewind now has to check
— not a replacement for OFAC, an *addition*.

The team pulls in the EU list and immediately finds a disagreement. Real, long-documented fact:
OFAC maintains a **comprehensive US embargo on Cuba**; the EU does not — the EU has run active
diplomatic and trade relations with Cuba for years and imposes nothing close to a comprehensive
block. So a Cuban-linked payment that OFAC's list says block outright is, on the EU list, simply...
absent. Nobody made an error. Two governments genuinely disagree, on purpose, as a matter of
foreign policy.

**The name for this:** **three embassies, three clipboards.** Picture three separate embassy
booths, each holding its own clipboard of names it personally won't clear — and none of them call
each other before updating their own clipboard. A traveler can be waved through by embassy A and
stopped cold by embassy B, and that's not a bug in either booth, it's just what happens when
independent authorities keep independent lists.

```mermaid
flowchart TB
    subgraph Booths["Three embassies, three clipboards"]
        E1["OFAC clipboard:\nCuba = comprehensive block"]
        E2["EU clipboard:\nCuba = not on it at all"]
        E3["UN clipboard:\nseparate designations again"]
    end
    T["Same Cuban-linked\npayment"] --> E1
    T --> E2
    T --> E3
    E1 --> R1["BLOCK"]
    E2 --> R2["ALLOW"]
    E3 --> R3["depends on the entity"]
```

The obvious question: *if the lists disagree, which one wins?* Tradewind doesn't have an answer
yet — right now they don't even have three separate clipboards, they've been trying to just add
EU entries into the *same* array as the OFAC entries to save time, which is about to bite them.

**How I'd say this in an interview:** "The moment you operate in more than one jurisdiction, you
don't have 'the sanctions list' anymore — you have several, run by different governments, on
different cadences, and they genuinely disagree by design, like Cuba under OFAC versus the EU.
That's not a data-quality bug to fix, it's a structural fact you have to design around."

---

## Chapter 4 — The stapled clipboard

Under deadline pressure, an engineer takes the fast path: merge OFAC's and the EU's entries into
one combined table at ingestion time, with a single `is_blocked` boolean per entity. Call this
**the stapled clipboard** — literally stapling embassy A's and embassy B's clipboards together and
writing just one "yes" or "no" per name on the front page, then throwing the original two pages
away.

It works, in the sense that payments still get blocked or allowed correctly. Six weeks later, a
compliance audit asks a very specific question about one blocked payment: *"which list actually
flagged this — OFAC or the EU list, and what was each one's individual verdict?"* Tradewind's
engineers open the database and find... one boolean. The staple already happened. There is no way
to reconstruct which of the two original clipboards said "block" and which said "allow," because
that information was thrown away the moment the two lists were merged.

```mermaid
flowchart LR
    subgraph Before["Before the staple"]
        O["OFAC: BLOCK"]
        Eu["EU: ALLOW"]
    end
    Before -->|"merged at ingestion"| S["Stapled: is_blocked = true"]
    S -.->|"audit asks: which list said block?"| Q["❓ — that information\nis already gone"]
```

This isn't a hypothetical concern in this industry — sanctions enforcement settlements routinely
come with detailed record-keeping and monitoring requirements attached; HSBC's 2012 deferred
prosecution agreement (a real, documented $1.9 billion settlement) explicitly required years of
enhanced, auditable transaction-monitoring going forward. A system that can't say which source
drove a decision fails exactly the kind of audit that real settlements actually demand.

**The fix:** un-staple. Keep every source's data, and every source's individual verdict, in its own
separate lane, forever — never merge two sources' answers into one flag at ingestion time. This is
the direct opposite instinct from Chapter 3's shortcut, and it's the one Tradewind commits to for
good: **N independent sources means N independent pipelines and N independently-stored verdicts,
combined only at the very last step, and even then without throwing the individual answers away.**

**New problem, immediately visible once sources are kept separate:** now that OFAC and the EU list
both exist as their own lookups, what stops Tradewind from just checking *every* source against
*every* payment, all the time, regardless of whether that payment has anything to do with the EU
or the US at all?

**How I'd say this in an interview:** "Merging independent sources into one flag at ingestion time
destroys attribution permanently — you genuinely cannot answer 'which list said block' after the
fact, and that's a hard requirement in this domain, not a nice-to-have. The fix is to keep every
source's pipeline, index, and verdict separate all the way through, and only combine them at
decision time, never before."

---

## Chapter 5 — Checking every booth for every traveler

With OFAC and the EU list now kept properly separate, Tradewind does the simplest correct-looking
thing: check *both* sources against *every* payment, every time. A purely domestic Belgian
merchant paying a purely domestic Belgian buyer, in euros, with zero US or Cuban connection
whatsoever, still gets run through OFAC's Cuba-embargo logic — because nobody told the system it
didn't need to.

This is wasteful, but the actually dangerous version of the same mistake goes the other direction.
Tradewind adds the **UN Security Council Consolidated List** as a third source. A UN designation
against an entity — which UN member states are independently obligated to enforce on their own
transactions — gets applied to a purely intra-EU transfer with **no connection to that designated
entity's jurisdiction at all**, purely because the entity's name happens to loosely match. Worked
number: this generates roughly **340 unnecessary payment holds per week** `[illustrative]` that a
review team has to manually clear, each one a legitimate transaction delayed for no real
jurisdictional reason.

```mermaid
flowchart TD
    A["Domestic Belgium -> Belgium\ntransfer, EUR only"] --> B["Checked against\nOFAC anyway"]
    A --> C["Checked against\nEU list anyway"]
    A --> D["Checked against\nUN list anyway"]
    D -->|"coincidental name match,\nno real jurisdictional link"| E["Held for review\n— wasted, and wrong"]
```

The obvious question: *isn't checking every source at least safe, even if it's wasteful?* No — and
this is the part that trips people up. A source with no real legal claim on a transaction
shouldn't be allowed to block it just because a name happened to match, any more than a UN
designation obligates enforcement against a purely domestic transfer between two parties in a
country with no connection to that designation. **Applying a source that doesn't actually apply is
a wrong answer, not just wasted computation.**

**How I'd say this in an interview:** "Checking every source against every transaction feels safe,
but it's wrong in both directions — it's wasteful for the sources that obviously don't apply, and
it's a real correctness bug when a source with no actual jurisdictional claim ends up blocking a
transaction it never had legal authority over. The fix has to be figuring out *which* sources
actually apply to *this* transaction, before checking any of them."

---

## Chapter 6 — Three witnesses, three addresses, and the dollar that always walks through the US booth

So: which sources apply to a given payment? Tradewind's first instinct is "check the payer's
country and the payee's country" — but even that turns out to be ambiguous. A payer's billing
address says Argentina. Their IP address geolocates to Brazil (a VPN, or just an imprecise
geolocation database). Their bank account is domiciled in the US. Ask three different signals
"what country is this payer in" and get three different, all individually defensible, answers.
Call this **three witnesses, three addresses** — and it's a real, recurring headache in this space,
not a corner case, since none of billing address, IP, and bank-account country are guaranteed to
agree, and a bad actor can deliberately make them disagree.

```mermaid
flowchart LR
    P["Same payer"] --> W1["Billing address\nsays Argentina"]
    P --> W2["IP address\ngeolocates to Brazil"]
    P --> W3["Bank account\ndomiciled in the US"]
    W1 -.-> Q["Which one is\n'the' country?"]
    W2 -.-> Q
    W3 -.-> Q
```

Then a second, sharper problem surfaces on top of the first. A payment moves money from a company
in Turkey to a company in Brazil — neither party American — but it's denominated and cleared in
US dollars, routed through a US correspondent bank. Real, documented legal fact: because of how
dollar-clearing routes through the US financial system, **OFAC's jurisdiction can attach to this
transaction regardless of where either transacting party is located.** Call this **the dollar
always walks through the US booth** — it doesn't matter whose passport the money is carrying, if
it's dollars clearing through a US bank, it has to go through the US checkpoint too.

```mermaid
sequenceDiagram
    participant Payer as Turkey-based payer
    participant Bank as US correspondent bank
    participant Payee as Brazil-based payee
    Note over Payer,Payee: USD-denominated payment
    Payer->>Bank: send USD
    Bank->>Payee: forward USD
    Note over Bank: OFAC jurisdiction attaches HERE,\nregardless of Turkey or Brazil
```

Most candidates, and Tradewind's own first design, miss this entirely — they check party
countries and stop, never noticing that currency-clearing jurisdiction is its own, separate trigger.
Missing it means a genuinely OFAC-applicable transaction between two non-US parties would sail
through unscreened.

**The fix:** build an explicit **jurisdiction-nexus resolver** — a small, rules-based mapping,
written and reviewed by legal/compliance (never inferred by pattern-matching), from transaction
attributes — payer country, payee country, clearing currency, routing path — to the *set* of
sources that actually have a legal claim on this specific payment. USD clearing through a US bank
adds OFAC to that set unconditionally. Party country adds each party's own local regulator and any
supranational source their country is obligated to enforce.

```mermaid
flowchart TD
    A["Payment: payer country,\npayee country, clearing\ncurrency, routing"] --> B{"USD clearing via\nUS correspondent bank?"}
    B -->|yes| C["Add OFAC\n— regardless of party countries"]
    B -->|no| D["Skip OFAC"]
    A --> E{"Either party's country\nobligated under UN list?"}
    E -->|yes| F["Add UN source"]
    A --> G{"Either party's country\nhas own local regulator?"}
    G -->|yes| H["Add that regulator"]
    C --> I["Final applicable\nsource set"]
    D --> I
    F --> I
    H --> I
```

**New problem, right behind this one:** knowing which sources apply is only half the job — those
applicable sources can still individually disagree about the *verdict* for this specific payer or
payee, exactly like Chapter 3's Cuba example, just now scoped down to one transaction instead of a
whole country.

**How I'd say this in an interview:** "Figuring out 'the' country of a transaction is genuinely
ambiguous — billing address, IP, and bank domicile can all disagree — and on top of that, currency
clearing creates its own jurisdiction: a USD payment between two non-US parties can still be OFAC's
business because of how dollar-clearing routes through US correspondent banks. That's the single
most commonly missed nuance in this whole space, and the fix is an explicit, legally-reviewed
rules engine mapping transaction attributes to applicable sources — never an inferred guess."

---

## Chapter 7 — Guards don't vote, they veto

Back to the Turkey-to-Brazil, USD-cleared payment. Jurisdiction-nexus resolution correctly says
both OFAC (because of USD clearing) and Turkey's local regulator (because of the payer's home
country) apply. Tradewind screens against both. OFAC's index returns a hit — the payee matches a
designated entity. Turkey's local regulator returns clean. Two applicable sources, one says block,
one says allow. What does Tradewind actually do?

The tempting wrong answers: average them somehow (meaningless — there's no numeric middle ground
between "sanctioned" and "not"), or pick whichever source feels more authoritative and ignore the
other (arbitrary, and indefensible on audit). The actual answer, and the name for it: **guards
don't vote, they veto.** Picture the three embassy booths from Chapter 3 again — if *any one* guard
says stop, the traveler stops, full stop, regardless of what the other two guards think. Guards
don't hold an election; any single applicable veto ends it.

```mermaid
flowchart TD
    A["Applicable sources'\nverdicts collected"] --> B{"Any applicable\nsource says BLOCK?"}
    B -->|yes| C["Final decision: BLOCK\n— attribute to OFAC specifically,\nTurkey's regulator's ALLOW stays visible too"]
    B -->|"no, all ALLOW"| D["Final decision: ALLOW"]
```

Why this is the correct default, not just the cautious one: allowing a payment that even one
legally-applicable regulator would block is a direct violation of that regulator's rules, full
stop — it doesn't matter that a second regulator was fine with it, because that second regulator's
approval carries no authority to excuse a violation of the first. This is the same asymmetric-cost
logic behind BNP Paribas's $8.9 billion fine: the cost of one confirmed violation dwarfs the cost
of occasionally holding a payment that, on reflection, could have gone through.

One thing worth being precise about: this is *not* the same as one court order overturning an
earlier one. Independent sanctions sources don't supersede each other — OFAC's designation and
Turkey's own regulator's clearance are two separate, simultaneously-valid obligations, not two
competing rulings on the same case. The rule is "all applicable sources' obligations apply at
once," never "the newer or stricter one legally cancels the other out."

**New problem:** the decision itself is now correct and defensible. But it's built from data that
isn't equally fresh — OFAC's list was pulled an hour ago, Turkey's local regulator's list was
pulled a week ago, and the team is about to make the mistake of reporting one blended "our data is
at most X stale" number to compliance.

**How I'd say this in an interview:** "When applicable sources disagree, the default has to be:
any one applicable source saying block wins, full attribution kept for both the source that
blocked it and the ones that didn't. That's not the same as one order overriding another — these
are parallel, simultaneously-valid obligations, and there's no principled way to average or vote
across independent regulators."

---

## Chapter 8 — Three clocks on three walls

A well-meaning ops lead wants one clean number for the compliance dashboard: "our sanctions data is
at most 4 hours stale." The team blends OFAC's hourly pull, the EU list's daily pull, and the UN
list's *genuinely irregular* publishing cadence — the UN Security Council only issues new
designations when the Security Council actually acts, which is not a fixed schedule at all — into
one averaged number.

Two weeks later this blended number causes two opposite failures on the same day. First: the UN
list hasn't changed in **19 days**, not because anything's broken, but because there's simply been
no new Security Council action — yet the blended dashboard flags it "abnormally stale" and pages
someone for nothing. Second, the *actual* problem: the EU pipeline genuinely broke two days ago
after a file-format change on the EU's publishing side, and because it's buried inside one averaged
number alongside OFAC's healthy hourly pulls, nobody notices for those two full days.

**The name for this: three clocks on three walls.** Each source keeps its own clock, and they
don't run at the same speed — OFAC's clock ticks roughly hourly, the EU's ticks roughly daily, and
the UN's clock has no fixed tick at all, it just says "time since the last actual designation."
Averaging three different clocks into one number produces a number that's wrong about all three.

```mermaid
flowchart LR
    A["OFAC: hourly pulls,\n~1h typical staleness"] --> AGG["Per-decision disclosure:\n'OFAC as of T-1h,\nEU as of T-2d (broken!),\nUN as of T-19d (normal — no news)'"]
    B["EU: daily pulls,\nnow 2 DAYS stale — broken"] --> AGG
    C["UN: event-driven,\nno fixed cadence at all"] --> AGG
```

**The fix:** track and disclose staleness **per source**, never blended into one figure. Each
decision's response should be able to show "here's how current each individual source was when
this specific verdict was made" — the same attribution instinct from Chapter 4, now applied to
freshness instead of to the verdict itself.

**New problem:** tracking staleness per source is only useful if the system actually *does*
something sensible when one source's staleness crosses from "normal" into "broken" — right now,
nothing distinguishes "the UN simply hasn't acted" from "our EU pipeline is down," and nothing
decides whether a broken source should stop screening altogether.

**How I'd say this in an interview:** "Don't blend staleness across sources with genuinely
different cadences — a source that updates rarely because nothing's happened isn't the same kind
of stale as a source that's supposed to update daily and silently stopped. Track and disclose
staleness per source, so 'is this decision based on current data' is always answerable per source,
not as one misleading average."

---

## Chapter 9 — One booth's fax machine breaking

The EU pipeline's silent two-day break from Chapter 8 turns out to be the beginning of a longer
outage — the EU's publishing side changed their file format, and Tradewind's parser has been
silently failing for **4 days** before anyone notices, because nobody built an alert for "this
specific source stopped updating." The question that actually matters now: while the EU pipeline
is down, should Tradewind **stop screening every payment that needs an EU-list check**, or keep
going using the EU list's last known-good snapshot?

**The name for this: one booth's fax machine breaking.** If the EU embassy booth's fax machine
jams and it can't get today's updated clipboard, that doesn't mean every other booth stops
checking travelers too — it means that *one* booth is working from yesterday's clipboard until its
fax gets fixed, and everyone should be told that specific fact, but the checkpoint as a whole keeps
operating.

```mermaid
sequenceDiagram
    participant Txn as Payment (needs OFAC + EU checks)
    participant OFAC as OFAC index (healthy)
    participant EU as EU index (4 days stale, broken pipeline)
    participant Agg as Aggregation

    Note over EU: pipeline down 4 days — parser failing silently
    Txn->>OFAC: screen
    OFAC-->>Agg: ALLOW (fresh)
    Txn->>EU: screen
    EU-->>Agg: ALLOW (last known-good, 4 days stale)
    Agg-->>Txn: decision = ALLOW, EU staleness of 4 days disclosed
    Note over Agg: EU's outage degraded ONE input's freshness —\nit did not halt screening for this payment
```

Concretely, this means: a payment needing only OFAC and no EU nexus is completely unaffected by
the EU outage. A payment that does need the EU list still gets a decision — using the EU list's
last known-good snapshot, with that 4-day staleness explicitly disclosed in the response — rather
than the whole screening call failing or, worse, silently skipping the EU check with no record
that it was skipped.

One related, smaller wrinkle worth naming here rather than a whole separate chapter: OFAC's and the
EU's entries for the *same* real-world entity sometimes have slightly different spellings or dates
of birth, since each source is independently maintained. Treating those as two unrelated entities
because the strings don't match exactly is its own gap — the fix borrows the fuzzy-matching
approach used for individual-entity screening rather than assuming any two sources will agree on
exact spelling.

**How I'd say this in an interview:** "One source's pipeline breaking should degrade *that
source's* contribution only, disclosed clearly, never gate every decision that doesn't actually
need it, and not silently gate the ones that do either. It's the same fail-open instinct as any
single-source pipeline, just applied per source instead of globally, plus a monitor that actually
watches each source's own health independently."

---

## Where the story lands

```mermaid
flowchart LR
    A["Ch1: hardcoded list\n(stale forever, silently)"] -->|"fixes: automate the pull\nbreaks: still a staleness window"| B["Ch2: the photocopy"]
    B -->|"fixes: shrink the window\nbreaks: one source isn't enough"| C["Ch3: three embassies,\nthree clipboards"]
    C -->|"fixes: separate sources\nbreaks: merged at ingestion, staple"| D["Ch4: un-staple,\nkeep attribution"]
    D -->|"fixes: attribution kept\nbreaks: checking every source, always"| E["Ch5: applicability matters"]
    E -->|"fixes: figure out which\nsources apply\nbreaks: 'country' itself is ambiguous"| F["Ch6: jurisdiction-nexus\nresolver + long-arm currency"]
    F -->|"fixes: right applicable set\nbreaks: applicable sources disagree"| G["Ch7: guards veto,\nnever vote"]
    G -->|"fixes: deterministic decision\nbreaks: one blended staleness number"| H["Ch8: three clocks,\nthree walls"]
    H -->|"fixes: per-source staleness\nbreaks: what if a source dies"| I["Ch9: fail-open per source"]
```

```mermaid
mindmap
  root((Multi-source sanctioned-\ncountry payment blocking))
    Staleness
      hardcoded list never expires loudly
      automate the pull, per source
      never blend staleness across sources
    Multiple sources
      independent regulators disagree by design
      never merge them at ingestion
      keep every verdict, per source, forever
    Applicability
      checking every source is wasteful AND wrong
      country itself is ambiguous — billing, IP, bank
      currency clearing creates its own jurisdiction
    Conflict resolution
      any applicable source blocking wins
      sources run in parallel, never supersede
    Degradation
      one source's outage isolates to that source
      disclose staleness, never hide a skipped check
```

The skill isn't reciting all nine chapters in every interview — it's knowing which ones the
requirements actually demand. A single-market payments company screening against one list can
reasonably stop around Chapter 2. The moment "multiple regulators" or "USD clearing" enters the
prompt, Chapters 3 through 7 aren't optional depth, they're the actual question being asked.

---

## Grill me — adversarial follow-ups

**Q1: "Why not just always screen against every source, all the time — isn't that the safest
option?"**
It feels safest but it isn't — applying a source with no real jurisdictional claim on a
transaction is a wrong answer, not just wasted work, the same way a UN designation has no
authority to block a purely domestic transfer with no connection to it. Safety here comes from
applying the *right* sources correctly, not from applying all of them indiscriminately.

**Q2: "Isn't 'any applicable source blocking wins' just going to make this system block way too
much?"**
Some over-blocking is the accepted cost, and it's a much cheaper cost than a confirmed violation —
a wrongly-held payment goes to a review queue and usually clears in hours, while a confirmed
sanctions violation is a multi-billion-dollar regulatory event, as BNP Paribas's case shows. The
asymmetry is real and it's the entire justification for the veto rule.

**Q3: "Walk me through why a USD payment between a Turkish company and a Brazilian company would
ever be OFAC's business."**
Because of how dollar clearing works — a USD-denominated payment routes through the US financial
system via a US correspondent bank, and that routing is what creates OFAC's jurisdiction, entirely
independent of where either transacting party is located. This is a real, documented legal
principle, and it's the single most commonly missed nuance in this space.

**Q4: "If OFAC and the EU list disagree about Cuba, doesn't that mean one of them is just wrong?"**
No — they're both correct under their own government's foreign policy; the US maintains a
comprehensive embargo on Cuba and the EU doesn't, and neither authority is obligated to match the
other. The system's job isn't to resolve that disagreement into one "true" answer, it's to apply
each source correctly wherever it actually has jurisdiction.

**Q5: "Why keep every source's own verdict instead of just storing the final aggregate decision —
isn't that simpler?"**
Because the first time a real audit or investigation asks "which specific list flagged this
payment," a system that only stored the final boolean has no answer — that information was thrown
away the moment sources were merged, and there's no reconstructing it after the fact. Attribution
has to be designed in from the start, not bolted on later.

**Q6: "How is jurisdiction-nexus resolution different from just checking the payer's and payee's
country?"**
Party country is only one input — currency-clearing jurisdiction is a separate, independent
trigger that can apply regardless of where either party is, and even "party country" itself is
ambiguous between billing address, IP geolocation, and bank domicile. Nexus resolution has to be an
explicit rules engine covering all of those inputs, reviewed by legal, not an inferred shortcut
based on party country alone.

**Q7: "What happens if one source's pipeline is completely down — do you just block everything that
might need it, to be safe?"**
No — that turns one source's outage into a system-wide outage, which is a worse failure than the
one you're trying to avoid. The right answer is to keep screening using that source's last
known-good snapshot, disclose its staleness explicitly in the decision, and only genuinely alarm
when staleness crosses a monitored threshold for that specific source.

**Q8: "Two sources list the same real entity with a slightly different spelling — does the system
treat them as different people?"**
It shouldn't, and this is a real gap if you assume exact-identifier matching across independently
maintained sources — the fix borrows the same fuzzy-matching approach used for individual entity
screening, applied per source, rather than assuming any two government lists will agree on exact
spelling or formatting.

**Q9: "Given everything in this story, where do you actually start if someone just says 'design
sanctions blocking for a payments company' cold?"**
First question: is this one source or genuinely multiple regulators — that decides whether you
need any of the nexus/conflict machinery at all. If it's multiple sources, say up front that you'll
keep them separate through ingestion and attribution, resolve applicability before checking
anything, and default to any-applicable-source-blocks — then go as deep as the interviewer asks
into currency-clearing jurisdiction or staleness tracking.

**Q10: "Isn't jurisdiction-nexus resolution just a fancy way of avoiding doing the actual sanctions
check on some transactions?"**
It's the opposite — without it, you either over-apply sources that have no legal claim on a
transaction (a real correctness bug, not just waste) or, worse, under-apply a source that does
apply, like missing OFAC's currency-clearing jurisdiction entirely. Nexus resolution is what makes
sure the *right* sources get checked, not fewer sources getting checked.

---

## Cheat sheet — one line per stop on the story

- **Hardcoded sanctions list**: never expires loudly — automate the pull from the actual source on
  a schedule, and monitor the list itself, not just the payments running against it.
- **The photocopy**: pulling on a schedule shrinks the staleness window but never removes it — make
  the window small, monitored, and disclosed, never pretend it's zero.
- **Multiple independent sources**: each regulator keeps its own list, on its own cadence, and they
  genuinely disagree by design (OFAC's Cuba embargo vs. the EU's, for example) — that's a structural
  fact, not a data-quality bug.
- **Never merge sources at ingestion**: stapling multiple sources into one boolean destroys
  attribution permanently — keep every source's pipeline, index, and verdict separate all the way
  through.
- **Checking every source against every transaction is wrong, not just wasteful**: a source with no
  real jurisdictional claim on a payment shouldn't be able to block it just because a name happened
  to match.
- **"The country" of a transaction is genuinely ambiguous**: billing address, IP geolocation, and
  bank domicile can all disagree — jurisdiction-nexus resolution has to account for all of them.
- **Currency-clearing jurisdiction is its own trigger**: a USD payment between two non-US parties
  can still be OFAC's business because of how dollar clearing routes through US correspondent
  banks — the most commonly missed nuance in this space.
- **Any applicable source blocking wins**: independent regulators' obligations run in parallel, not
  by vote or average — and they don't supersede each other the way one court order can override
  another.
- **Track staleness per source, never blended**: different cadences (hourly, daily, event-driven)
  carry genuinely different, non-comparable meanings of "how stale is this."
- **One source's outage isolates to that source**: screen using its last known-good snapshot with
  disclosed staleness — never let one source's trouble halt decisions that don't need it, or
  silently skip the ones that do.
