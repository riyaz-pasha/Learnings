# Base62

**Base62** is a positional numeral system with 62 symbols (digits). A common alphabet is:

```
ALPH62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
```

Indices: `'0' -> 0`, `'1' -> 1`, … `'9' -> 9`, `'A' -> 10`, … `'Z' -> 35`, `'a' -> 36`, … `'z' -> 61`.

Each Base62 character encodes `log2(62) ≈ 5.9541963104` bits of information.

---

# Encoding (integer → Base62 string) — algorithm & step-by-step

General algorithm (classic repeated division):

1. If `N == 0` return `ALPH62[0]` (i.e. `"0"`).
2. Repeatedly:

   * `rem = N % 62`
   * `digit = ALPH62[rem]`
   * push `digit` onto a list
   * `N = N // 62`
3. After loop, reverse the list and join to get the encoded string.

This is the same technique used to convert decimal → any base.

### Example 1 — Encode `N = 125` to Base62 (step-by-step)

Start `N = 125`.

1. `125 ÷ 62 = 2` remainder `1` → remainder `1` maps to `'1'`. Keep `'1'` (LSB).
2. `2 ÷ 62 = 0` remainder `2` → remainder `2` maps to `'2'`.
3. Stop (quotient 0). Collected digits (LSB→MSB) = `['1', '2']`. Reverse → `"21"`.

So **125 → `"21"`** in Base62.

### Example 2 — Encode `N = 1,000`

Start `N = 1000`.

1. `1000 ÷ 62 = 16` remainder `8` → remainder `8` → `'8'`.
2. `16 ÷ 62 = 0` remainder `16` → remainder `16` → `'G'` (since 10→A, 16→G).
3. Reverse `['8','G']` → `"G8"`.

So **1000 → `"G8"`**.

### Example 3 — Encode `N = 123,456,789` (multi-digit)

Start `N = 123,456,789`.

1. `123,456,789 ÷ 62 = 1,991,238` remainder `33` → `33` → `'X'`.
2. `1,991,238 ÷ 62 = 32,116` remainder `46` → `46` → `'k'`.
3. `32,116 ÷ 62 = 518` remainder `0` → `0` → `'0'`.
4. `518 ÷ 62 = 8` remainder `22` → `22` → `'M'`.
5. `8 ÷ 62 = 0` remainder `8` → `8` → `'8'`.
   Collected LSB→MSB: `['X','k','0','M','8']`. Reverse → **`"8M0kX"`**.

So **123,456,789 → `"8M0kX"`**.

---

# Decoding (Base62 string → integer) — algorithm & step-by-step

To decode a string `s`:

1. `value = 0`.
2. For each character `ch` in `s` from left to right:

   * `digit = index_of(ch)` in `ALPH62`
   * `value = value * 62 + digit`
3. After processing all characters, `value` is the integer.

### Decode the previous example `"8M0kX"`

Process left → right:

1. `'8'` → idx = 8 → `value = 0 * 62 + 8 = 8`.
2. `'M'` → idx = 22 → `value = 8 * 62 + 22 = 496 + 22 = 518`.
3. `'0'` → idx = 0 → `value = 518 * 62 + 0 = 32,116`.
4. `'k'` → idx = 46 → `value = 32,116 * 62 + 46 = 1,991,238`.
5. `'X'` → idx = 33 → `value = 1,991,238 * 62 + 33 = 123,456,789`.

Decoded value matches original integer.

---

# Short numeric properties (how many slugs of length L?)

`#slug(L) = 62^L`

Compute a few exact values (you can use these for capacity planning):

* `62^1 = 62`
* `62^2 = 62 × 62 = 3,844`
* `62^3 = 3,844 × 62 = 238,328`
* `62^4 = 238,328 × 62 = 14,776,336`
* `62^5 = 14,776,336 × 62 = 916,132,832`
* `62^6 = 916,132,832 × 62 = 56,800,235,584`
* `62^7 = 56,800,235,584 × 62 = 3,521,614,606,208`

So for example:

* If you need \~1 billion unique slugs, `62^5 = 916,132,832` is just under 1B; **use length 6** (62^6 ≈ 56.8B) to be safe.
* To encode arbitrary 64-bit integers you need **11 characters** (because 62^10 < 2^64 ≤ 62^11).

(Those are exact multiplications shown stepwise above.)

---

# Code (clear, production-ready pseudocode)

**Python (encode / decode)**

```python
ALPH62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

def base62_encode(n: int) -> str:
    if n == 0:
        return ALPH62[0]
    base = 62
    out = []
    while n > 0:
        n, rem = divmod(n, base)
        out.append(ALPH62[rem])
    return ''.join(reversed(out))

def base62_decode(s: str) -> int:
    idx = {c:i for i,c in enumerate(ALPH62)}
    val = 0
    for ch in s:
        val = val * 62 + idx[ch]
    return val
```

These are O(L) time where L is slug length (very fast).

---

# How Base62 is used in a URL shortener (concrete flow)

Common pattern (Sequential ID + Base62):

1. **Generate a globally unique numeric ID** for each new short URL. This might be:

   * An auto-incremented integer (central DB) OR
   * A distributed ID (Snowflake / ULID / KSUID) OR
   * Per-shard allocated ranges or auto-increment with step offsets.
2. **Encode** that numeric ID with Base62:

   ```text
   slug = base62_encode(global_id)
   ```
3. **Store** mapping in DB: `id (global_id) -> long_url, owner, meta`.
4. **Return** `https://short.domain/{slug}` to the user.

**Redirect (read) flow**:

1. Client requests `GET /{slug}`.
2. Server **decodes** slug → `id = base62_decode(slug)`.
3. **Find** the DB shard that holds `id` (if shard info is embedded in the ID or via routing).
4. Query DB: `SELECT long_url FROM urls WHERE id = id`.
5. Respond `HTTP 301 Location: long_url`.

Important: **Base62 itself guarantees no collisions** — it is a bijection between nonnegative integers and Base62 strings. Collisions only happen if the numeric ID generation has collisions or you use truncated hash outputs.

---

# Embedding shard info (example with Snowflake layout)

If you use a Snowflake-like 64-bit integer with fields:

```
global_id = (timestamp << (shard_bits + sequence_bits))
          | (shard_id << sequence_bits)
          | sequence
```

Example allocation: `timestamp:41 bits`, `shard_id:10 bits`, `sequence:12 bits`. That means:

* `sequence_bits = 12` → sequence values 0..4095 (IDs per ms per shard).
* `shard_bits = 10` → shard IDs 0..1023.
* To extract `shard_id` from a decoded `global_id`:

```
shifted = global_id >> sequence_bits   # removes sequence
shard_id = shifted & ((1 << shard_bits) - 1)
```

**Numeric example (worked):**

* Choose `timestamp = 1`, `shard_id = 5`, `sequence = 123`.
* Compute `global_id = (1 << (10+12)) | (5 << 12) | 123`.

  * `1 << 22 = 4,194,304`
  * `5 << 12  = 20,480`
  * `sum = 4,194,304 + 20,480 + 123 = 4,214,907`
* `slug = base62_encode(4,214,907)` → `"HgUN"` (example).
* On read:

  * `decoded = base62_decode("HgUN") = 4,214,907`.
  * `shifted = 4,214,907 >> 12 = floor(4,214,907 / 4096) = 1,029` (because `4096 * 1029 = 4,214,784` with remainder 123).
  * `mask = (1 << 10) - 1 = 1023`.
  * `shard_id = 1,029 & 1023 = 1,029 % 1024 = 5`.
* That recovers the shard\_id quickly and deterministically.

So Base62 is just a compact textual representation of the numeric `global_id`; you decode to get the numeric id, then extract shard bits.

---

# Practical considerations & gotchas

* **Case sensitivity**: Base62 is case-sensitive. Paths on the web are generally case-sensitive — ensure your front door (CDN / LB / web server) preserves case. Avoid any proxy that lowercases paths.
* **URL safety**: Base62 characters (`0-9A-Za-z`) are safe in URL path segments; no percent-encoding required.
* **Predictability**: Sequential IDs encoded in Base62 are predictable (you can enumerate short URLs). If you want privacy/unguessability, either:

  * Use random IDs (and Base62 encode them), or
  * Obfuscate (XOR with secret, then encode) so sequence order is not visible.
* **Fixed vs variable length**: Base62 yields variable-length slugs (shorter for small IDs). If you want fixed-length slug for aesthetic/UX reasons, left-pad with `ALPH62[0]` (e.g., `'0'`) to fixed length — decoding ignores leading zeros.
* **Custom aliases**: They may not be strictly Base62 (users often want `-` or `_` or case-insensitive strings). Treat custom aliases separately — map alias->global\_id in a lookup table (cache), then proceed as above.
* **Slug collisions**: If you derive slug by hashing + truncating and then Base62 encoding the truncated bits, you must handle collisions via DB check + retry. But if slug is just Base62 of a unique numeric id, no collision can occur due to encoding.
* **Storage/length planning**: choose slug length L based on expected total IDs:

  * For \~1B IDs → L = 6 (since 62^6 ≈ 56.8B).
  * To encode up to 2^64 numbers → L = 11.

---

# Short summary & recommendation

* **Base62 = compact, URL-safe, invertible text encoding of integers.**
* Use **Base62 encoding of a globally-unique integer id** (Snowflake or per-shard IDs). This gives short slugs, O(1) decode, and deterministic shard routing by bit extraction.
* For privacy, obfuscate the integer before encoding or use random IDs.
* Always preserve case and avoid proxies that mangle path casing.
* Use a separate global alias lookup for custom user aliases.

---
