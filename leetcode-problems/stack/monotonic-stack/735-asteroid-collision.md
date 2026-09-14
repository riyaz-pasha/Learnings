# ☄️ Asteroid Collision — Monotonic Stack Explained

---

## 1️⃣ How to get the IDEA (interview thinking)

### Read the constraints carefully 👀

Key lines:

* “Each asteroid moves at the same speed”
* “Sign represents direction”
* “Two asteroids moving in the same direction will never meet”

👉 This immediately implies:

* **Only opposite directions can collide**
* Specifically:

  ```
  right-moving (+)  meets  left-moving (-)
  ```

That means collisions only happen when:

```
stack top > 0   AND   current < 0
```

This single condition is the **big cue** for using a stack.

---

## 2️⃣ Why a stack is needed

Ask yourself:

> “Can a current asteroid destroy multiple previous asteroids?”

Yes:

```
[5, 10, -20]
```

`-20` destroys both `10` and `5`.

👉 Whenever **one future element can invalidate multiple previous elements**,
👉 **monotonic stack** is the correct tool.

---

## 3️⃣ What the stack represents

The stack stores:

* Asteroids that are **still alive**
* In their **original order**

The stack is not about increasing/decreasing values —
it’s about **resolving conflicts**.

---

## 4️⃣ When does a collision happen?

Only when:

```
stack.peek() > 0   AND   current < 0
```

All other cases are safe:

* `+ +` → same direction
* `- -` → same direction
* `- +` → moving away

---

## 5️⃣ Collision rules (very important)

Let:

```
top = stack.peek()
curr = current asteroid
```

### Case 1: |top| < |curr|

```
top explodes
pop stack
continue checking
```

### Case 2: |top| == |curr|

```
both explode
pop stack
discard curr
stop
```

### Case 3: |top| > |curr|

```
curr explodes
discard curr
stop
```

That’s it. These 3 cases drive the whole solution.

---

## 6️⃣ Algorithm (Step-by-step)

For each asteroid `a` in array:

1. Assume `a` is alive
2. While:

   ```
   stack not empty
   AND stack.peek() > 0
   AND a < 0
   ```

   resolve collision
3. If `a` survives → push into stack

---

## 7️⃣ Dry Run (Example 4 — FULL)

### Input

```
[3, 5, -6, 2, -1, 4]
```

---

### Start

```
stack = []
```

---

### 3

```
push
stack = [3]
```

---

### 5

```
push
stack = [3, 5]
```

---

### -6

```
5 vs -6 → 5 explodes
stack = [3]

3 vs -6 → 3 explodes
stack = []

-6 survives → push
stack = [-6]
```

---

### 2

```
-6 and 2 → no collision (moving away)
push
stack = [-6, 2]
```

---

### -1

```
2 vs -1 → 2 > 1 → -1 explodes
stack = [-6, 2]
```

---

### 4

```
2 and 4 → same direction
push
stack = [-6, 2, 4]
```

---

### ✅ Final State

```
[-6, 2, 4]
```

---

## 8️⃣ Java Implementation (Clean & Interview-Ready)

```java
class Solution {
    public int[] asteroidCollision(int[] asteroids) {
        Stack<Integer> stack = new Stack<>();

        for (int a : asteroids) {
            boolean alive = true;

            while (alive && !stack.isEmpty()
                   && stack.peek() > 0 && a < 0) {

                int top = stack.peek();

                if (Math.abs(top) < Math.abs(a)) {
                    stack.pop();        // top explodes
                } else if (Math.abs(top) == Math.abs(a)) {
                    stack.pop();        // both explode
                    alive = false;
                } else {
                    alive = false;      // current explodes
                }
            }

            if (alive) {
                stack.push(a);
            }
        }

        int[] result = new int[stack.size()];
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = stack.pop();
        }
        return result;
    }
}
```

---

## 9️⃣ Time & Space Complexity

* **Time:** `O(n)`
  (each asteroid pushed & popped once)
* **Space:** `O(n)` stack

---

## 🔑 How to IDENTIFY this problem as Monotonic Stack

### Cues to watch for:

* “Collisions”
* “Exploding / destroying previous elements”
* “Only certain direction combinations interact”
* “Chain reactions”

👉 These scream:

> **“Resolve conflicts with a stack”**

---

## 🧠 Is this NGE / PGE?

**Neither directly.**

This is a **collision-resolution stack**, but the idea is similar to:

* NGE-style popping
* Multiple pops per element

You can think of it as:

> “A conditional monotonic stack based on direction”

---

## 🧠 One-line Memory Hook

> **“Only (+) meets (-); resolve until someone survives.”**

