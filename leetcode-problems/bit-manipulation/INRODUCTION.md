# Bit Manipulation

### AND ( & )
* If both bits are 1 then resulting bit is 1 or else 0.
  
| A   | B   | Result |
| --- | --- | ------ |
| 0   | 0   | 0      |
| 0   | 1   | 0      |
| 1   | 0   | 0      |
| 1   | 1   | 1      |

### OR ( | )
* If any bit is 1 then resulting bit is 1 or else 0.
  
| A   | B   | Result |
| --- | --- | ------ |
| 0   | 0   | 0      |
| 0   | 1   | 1      |
| 1   | 0   | 1      |
| 1   | 1   | 1      |


### XOR ( ^ )
* If bits differ then resulting bit is 1 or else 0.
  
| A   | B   | Result |
| --- | --- | ------ |
| 0   | 0   | 0      |
| 0   | 1   | 1      |
| 1   | 0   | 1      |
| 1   | 1   | 0      |

---

## 1️⃣ Left Shift (`<<`)

### Syntax
```java
a << n
````

### Meaning

* Shifts bits of `a` **left by `n` positions**
* **Zeros are added on the right**
* Equivalent to **multiplying by `2ⁿ`**

### Example

```java
int a = 5;          // 00000000 00000000 00000000 00000101
int result = a << 1;
```

**Binary**

```
5       → 00000101
5 << 1  → 00001010
```

**Decimal**

```
5 << 1 = 10
```

### Another Example

```java
int x = 3;
System.out.println(x << 2);  // 12
```

👉 `3 × 2² = 12`

---

## 2️⃣ Signed Right Shift (`>>`)

### Syntax

```java
a >> n
```

### Meaning

* Shifts bits **right by `n` positions**
* **Preserves the sign bit** (leftmost bit)
* Positive numbers → fill with `0`
* Negative numbers → fill with `1`
* Equivalent to **dividing by `2ⁿ`** (rounded down)

---

### Example (Positive Number)

```java
int a = 10;         // 00001010
int result = a >> 1;
```

**Binary**

```
10      → 00001010
10 >> 1 → 00000101
```

**Decimal**

```
10 >> 1 = 5
```

---

### Example (Negative Number)

```java
int a = -8;
System.out.println(a >> 1);
```

**Binary (32-bit)**

```
-8      → 11111111 11111111 11111111 11111000
-8 >> 1 → 11111111 11111111 11111111 11111100
```

**Decimal**

```
-8 >> 1 = -4
```

👉 Sign bit (`1`) is preserved.

---

## 3️⃣ Unsigned Right Shift (`>>>`)

### Syntax

```java
a >>> n
```

### Meaning

* Shifts bits **right**
* **Always fills with `0`**
* Ignores sign bit
* Result is **always non-negative**

---

### Example (Positive Number)

```java
int a = 8;
System.out.println(a >>> 1);
```

**Result**

```
8 >>> 1 = 4
```

(Same as `>>` for positive numbers)

---

### Example (Negative Number – IMPORTANT)

```java
int a = -8;
System.out.println(a >>> 1);
```

**Binary**

```
-8        → 11111111 11111111 11111111 11111000
-8 >>> 1  → 01111111 11111111 11111111 11111100
```

**Decimal**

```
2147483644
```

👉 Sign bit replaced with `0`, so number becomes large positive.

---

## 4️⃣ Unsigned Left Shift ❌ (Does NOT Exist)

Java **does NOT have an unsigned left shift operator**.

### Why?

* Left shift (`<<`) already shifts in `0`
* No difference between signed & unsigned left shift

```java
a << n   // Always fills right side with 0
```

---

## 5️⃣ Summary Table

| Operator | Name                 | Fills With | Sign Preserved | Common Use              |
| -------- | -------------------- | ---------- | -------------- | ----------------------- |
| `<<`     | Left Shift           | `0`        | ❌              | Multiply by powers of 2 |
| `>>`     | Signed Right Shift   | `0` or `1` | ✅              | Divide by powers of 2   |
| `>>>`    | Unsigned Right Shift | `0`        | ❌              | Bit-level unsigned ops  |

---

## 6️⃣ Key Interview Notes ⭐

* `<<` → Multiply by `2ⁿ`
* `>>` → Divide by `2ⁿ` (keeps sign)
* `>>>` → Used when **sign must be ignored**
* No `<<<` operator in Java

---
