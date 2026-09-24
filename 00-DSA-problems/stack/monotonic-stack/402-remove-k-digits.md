# 🔢 Remove K Digits — Monotonic Stack (Deep Intuition)

---

## 1️⃣ What is the problem REALLY asking?

> Remove exactly `k` digits so that the resulting number is **as small as possible**, while keeping the **relative order** of remaining digits.

Key constraints hidden in the wording:

* Digits order **cannot change**
* We only **delete**, not rearrange
* Smaller digit at a **more significant position** matters more

---

## 2️⃣ The BIG CUE 🧠 (How to get the idea)

Ask yourself:

> “When does a digit make the number worse?”

Answer:

> When a **larger digit appears before a smaller digit**

Example:

```
1432219
 ↑
```

`4` before `3` is bad
`3` before `2` is bad

So we want:

```
digits in non-decreasing order
```

🚨 This sentence is the **monotonic stack trigger**.

---

## 3️⃣ Why a Monotonic INCREASING stack?

We want the number to be as small as possible:

* Keep smaller digits as left as possible
* Remove bigger digits when a smaller digit appears later

So we maintain a stack where:

```
stack bottom → top is increasing
```

If current digit is **smaller than stack top**, the stack top is hurting us.

---

## 4️⃣ The Core Greedy Rule (MOST IMPORTANT)

While:

```
stack not empty
AND k > 0
AND stack.peek() > current digit
```

👉 **Pop the stack**

Why?

* Removing a larger digit earlier gives a smaller number than removing something later

This is a **greedy + monotonic stack** combo.

---

## 5️⃣ Full Algorithm (Plain English)

For each digit `d` in `num`:

1. While stack is not empty, `k > 0`, and top digit > `d`:

   * Pop stack
   * `k--`
2. Push `d` into stack

After processing all digits:

* If `k > 0`, remove remaining digits from the **end** (largest ones)

Finally:

* Remove leading zeros
* If empty → return `"0"`

---

## 6️⃣ Dry Run (Example 1)

### Input

```
num = "1432219", k = 3
```

### Step-by-step

| Digit | Stack     | k | Action |
| ----- | --------- | - | ------ |
| 1     | [1]       | 3 | push   |
| 4     | [1,4]     | 3 | push   |
| 3     | [1]       | 2 | pop 4  |
|       | [1,3]     | 2 | push   |
| 2     | [1]       | 1 | pop 3  |
|       | [1,2]     | 1 | push   |
| 2     | [1,2,2]   | 1 | push   |
| 1     | [1,2]     | 0 | pop 2  |
|       | [1,2,1]   | 0 | push   |
| 9     | [1,2,1,9] | 0 | push   |

Result:

```
1219
```

---

## 7️⃣ Example 2 (Leading zeros)

```
num = "10200", k = 1
```

Process:

```
1 → push
0 → pop 1 (k=0), push 0
2 → push
0 → push
0 → push
```

Stack: `"0200"` → remove leading zeros → `"200"`

---

## 8️⃣ Example 3 (Remove all digits)

```
num = "10", k = 2
```

Everything removed → return `"0"`

---

## 9️⃣ Java Implementation (Interview-Ready)

```java
class Solution {
    public String removeKdigits(String num, int k) {
        Stack<Character> stack = new Stack<>();

        for (char c : num.toCharArray()) {
            while (!stack.isEmpty() && k > 0 && stack.peek() > c) {
                stack.pop();
                k--;
            }
            stack.push(c);
        }

        // Remove remaining digits from end
        while (k > 0 && !stack.isEmpty()) {
            stack.pop();
            k--;
        }

        // Build result
        StringBuilder sb = new StringBuilder();
        while (!stack.isEmpty()) {
            sb.append(stack.pop());
        }
        sb.reverse();

        // Remove leading zeros
        int i = 0;
        while (i < sb.length() && sb.charAt(i) == '0') {
            i++;
        }

        String result = sb.substring(i);
        return result.isEmpty() ? "0" : result;
    }
}
```

---

## 🔟 Time & Space Complexity

* **Time:** `O(n)`

  * Each digit pushed & popped once
* **Space:** `O(n)`

---

## 1️⃣1️⃣ How to IDENTIFY this as Monotonic Stack (Checklist)

If a problem says:

* “Remove digits”
* “Smallest / largest number”
* “Maintain order”
* “Greedy choice based on future digits”

👉 **Monotonic stack is the correct instinct**

---

## 1️⃣2️⃣ Is this NGE / PGE?

Not exactly — but it’s **NGE-style popping logic**.

You’re effectively saying:

> “If a smaller digit appears later, the previous larger digit is useless.”

That’s **Next Smaller Element behavior**, applied greedily.

---

## 🧠 One-line Memory Hook

> **“When a smaller digit appears, delete bigger digits on its left.”**
