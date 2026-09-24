### 🔧 Bitwise Operators Used

#### 1. `>>>` — **Unsigned Right Shift**

* Shifts bits to the right.
* Fills **leftmost bits with 0**, regardless of sign.
* Example:

  ```java
  int x = -2;        // Binary: 111...1110
  x >>> 1;           // Binary: 011...1111 (fills with 0 on the left)
  ```

#### 2. `<<` — **Left Shift**

* Shifts bits to the left.
* Fills **rightmost bits with 0**.
* Example:

  ```java
  int x = 3;         // Binary: 0000...0011
  x << 1;            // Binary: 0000...0110 (multiplies by 2)
  ```

#### 3. `&` — **Bitwise AND**

* Compares two bits: result is 1 if both are 1.
* Used to **mask (filter)** specific bits.
* Example:

  ```java
  1101 & 1011 = 1001
  ```

#### 4. `|` — **Bitwise OR**

* Compares two bits: result is 1 if either is 1.
* Used to **combine** results.
* Example:

  ```java
  1101 | 1011 = 1111
  ```

---

### 🧠 What's the Purpose of This Function?

This function **reverses the bits** of a 32-bit unsigned integer.
For example:

```java
Input:  00000010100101000001111010011100
Output: 00111001011110000010100101000000
```

---

### 💡 Idea: Divide and Conquer Bit Swapping

Instead of swapping each bit one-by-one, we:

* Divide the 32-bit number into halves, quarters, etc.
* Swap them layer by layer.

---

### 🧩 Step-by-Step Explanation

#### ✅ 1. Swap 16-bit Halves

```java
n = (n >>> 16) | (n << 16);
```

* Takes the left 16 bits and shifts them right.
* Takes the right 16 bits and shifts them left.
* Combines them — so left and right halves are swapped.

---

#### ✅ 2. Swap 8-bit Quarters

```java
n = ((n & 0xff00ff00) >>> 8) | ((n & 0x00ff00ff) << 8);
```

* `0xff00ff00` isolates bytes 1 and 3 (higher bits in each half).
* `0x00ff00ff` isolates bytes 0 and 2 (lower bits in each half).
* Then swaps each 8-bit quarter within the 16-bit halves.

---

#### ✅ 3. Swap 4-bit Groups

```java
n = ((n & 0xf0f0f0f0) >>> 4) | ((n & 0x0f0f0f0f) << 4);
```

* `0xf0f0f0f0` masks upper 4 bits of every byte.
* `0x0f0f0f0f` masks lower 4 bits of every byte.
* Swaps nibbles (4-bit pieces) within each byte.

---

#### ✅ 4. Swap 2-bit Pairs

```java
n = ((n & 0xcccccccc) >>> 2) | ((n & 0x33333333) << 2);
```

* `0xcccccccc` → binary: 11001100..., masks upper 2-bit pairs in every 4-bit group.
* `0x33333333` → binary: 00110011..., masks lower 2-bit pairs.
* Swaps them within each nibble.

---

#### ✅ 5. Swap Individual Bits

```java
n = ((n & 0xaaaaaaaa) >>> 1) | ((n & 0x55555555) << 1);
```

* `0xaaaaaaaa` → binary: 10101010..., masks even-indexed bits.
* `0x55555555` → binary: 01010101..., masks odd-indexed bits.
* Swaps each bit with its neighbor.

---

### ✅ Summary

This is a **divide and conquer** approach that efficiently reverses the bits using:

* Bitwise operations.
* Precomputed masks.
* No loops → constant time performance: **O(1)**.

