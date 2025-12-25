<details>

<summary><strong>1️⃣ Swapping Two Numbers Without a Third Variable (Using XOR)</strong></summary>

```java
a = a ^ b;
b = a ^ b;
a = a ^ b;
```

### Why This Works (Simple Explanation)

#### XOR Rules Used

* `x ^ x = 0`
* `x ^ 0 = x`
* XOR is reversible


## Step-by-Step

### Step 1

```java
a = a ^ b;
```

Now `a` stores **both values combined**:

```
a = a ^ b
b = b
```

---

### Step 2

```java
b = a ^ b;
```

Substitute `a`:

```
b = (a ^ b) ^ b
b = a ^ (b ^ b)
b = a
```

✔ `b` becomes **original `a`**

---

### Step 3

```java
a = a ^ b;
```

Substitute `b`:

```
a = (a ^ b) ^ a
a = b
```

✔ `a` becomes **original `b`**

---

## Final Result (Swapped)

```
a = original b
b = original a
```

---

## Key Idea (One Line)

> XOR cancels itself, so applying it twice with the same number recovers the original value.

---

## Interview Tip ⭐

* Works only for integers
* Fails if `a` and `b` refer to the same variable

---
</details>

---

<details>
<summary><strong>2️⃣ Checking if the i-th Bit is Set</strong></summary>

To check whether the **i-th bit (0-based index from right)** of a number is `1`, we commonly use **bit masking** or **bit shifting**.

---

## Method 1: Using Left Shift (Bit Mask)

```java
(1 << i) & num   // bit is set if result ≠ 0
````

### Why This Works

* `1 << i` creates a mask with only the i-th bit set
* `&` keeps only that bit from `num`
* If the result is not `0`, the bit was set

### Example

```java
int num = 10;   // 1010
int i = 1;

(1 << 1) = 0010
1010 & 0010 = 0010 ≠ 0  → bit is set
```

---

## Method 2: Using Right Shift

```java
(num >> i) & 1   // bit is set if result ≠ 0
```

### Why This Works

* `num >> i` brings the i-th bit to the **least significant position**
* `& 1` extracts that bit
* If result is `1`, the bit was set

### Example

```java
int num = 10;   // 1010
int i = 1;

num >> 1 = 0101
0101 & 0001 = 0001 ≠ 0  → bit is set
```

---

## Comparison

| Method           | How It Works | Common Use   |
| ---------------- | ------------ | ------------ |
| `(1 << i) & num` | Masking      | Most common  |
| `(num >> i) & 1` | Shifting     | Easy to read |

---

## Important Notes ⭐

* Bit index `i` starts from **0 (rightmost bit)**
* Works for integers (`int`, `long`)
* For negative numbers, prefer **masking method**

---

### Key One-Liner (Interview)

> To check the i-th bit, either mask it using `(1 << i)` or shift it to LSB using `>>` and AND with `1`.

</details>


---

<details>
<summary><strong>3️⃣ Setting the i-th Bit</strong></summary>

To **set (turn ON) the i-th bit** of a number, use the **bitwise OR (`|`) operator**.

---

## Formula

```java
num | (1 << i)
````

---

## Why This Works

* `1 << i` creates a mask with only the i-th bit set to `1`
* OR (`|`) with `1` always results in `1`
* All other bits remain unchanged

---

## Example

```java
int num = 10;   // 1010
int i = 0;
```

### Step-by-Step

```
1 << 0  = 0001
1010 | 0001
------------
1011  = 11
```

✔ The **0th bit** is now set

---

## Another Example

```java
int num = 8;   // 1000
int i = 1;
```

```
1 << 1  = 0010
1000 | 0010
------------
1010 = 10
```

✔ The **1st bit** is now set

---

## Key Properties

* If the bit is already `1`, value remains unchanged
* Operation is **safe and idempotent**

---

## Important Notes ⭐

* Bit positions are **0-based from the right**
* Works for `int` and `long`
* Very common in **bitmasking and permissions**

---

## Interview One-Liner 💡

> To set the i-th bit, OR the number with `(1 << i)`.

</details>

---

<details>
<summary><strong>4️⃣ Clearing the i-th Bit</strong></summary>

To **clear (turn OFF) the i-th bit** of a number, use **AND (`&`) with the inverted bit mask**.

---

## Formula

```java
num & ~(1 << i)
````

---

## Why This Works

* `1 << i` creates a mask with only the i-th bit set
* `~(1 << i)` flips all bits → i-th bit becomes `0`, others become `1`
* AND (`&`) with `0` clears the bit
* AND (`&`) with `1` keeps the bit unchanged

---

## Example

```java
int num = 10;   // 1010
int i = 1;
```

### Step-by-Step

```
1 << 1   = 0010
~0010    = 1101
1010 & 1101
-----------
1000  = 8
```

✔ The **1st bit** is cleared

---

## Another Example

```java
int num = 15;   // 1111
int i = 2;
```

```
1 << 2   = 0100
~0100    = 1011
1111 & 1011
-----------
1011 = 11
```

✔ The **2nd bit** is cleared

---

## Key Properties

* If the bit is already `0`, value remains unchanged
* Operation is **safe and idempotent**

---

## Important Notes ⭐

* Bit positions are **0-based from the right**
* Works for `int` and `long`
* Frequently used in **bit masks and flags**

---

## Interview One-Liner 💡

> To clear the i-th bit, AND the number with the inverted mask `~(1 << i)`.

</details>

---

<details>
<summary><strong>5️⃣ Toggling the i-th Bit</strong></summary>

To **toggle (flip) the i-th bit** of a number, use the **bitwise XOR (`^`) operator**.

---

## Formula

```java
num ^ (1 << i)
````

---

## Why This Works

* `1 << i` creates a mask with only the i-th bit set
* XOR (`^`) rules:

  * `x ^ x = 0`
  * `x ^ 0 = x`
  * `0 ^ 1 = 1`  → bit turns ON
  * `1 ^ 1 = 0`  → bit turns OFF
* All other bits remain unchanged

---

## Example

```java
int num = 10;   // 1010
int i = 1;
```

### Step-by-Step

```
1 << 1  = 0010
1010 ^ 0010
------------
1000  = 8
```

✔ The **1st bit** toggled from `1 → 0`

---

## Another Example

```java
int num = 8;    // 1000
int i = 1;
```

```
1 << 1  = 0010
1000 ^ 0010
------------
1010 = 10
```

✔ The **1st bit** toggled from `0 → 1`

---

## Key Properties

* `0 → 1`, `1 → 0`
* Applying toggle twice restores original value
* Only the chosen bit changes

---

## Important Notes ⭐

* Bit positions are **0-based from the right**
* Works for `int` and `long`
* Very common in **bit manipulation problems**

---

## Interview One-Liner 💡

> To toggle the i-th bit, XOR the number with `(1 << i)`.

</details>
