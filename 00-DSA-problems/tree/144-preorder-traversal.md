# 🌳 Morris Preorder Traversal — Deep Explanation

## 1️⃣ What problem Morris traversal solves

Normally, tree traversal needs:

* **Recursion** → uses call stack → O(n) space (worst case)
* **Explicit Stack** → also O(n) space

👉 **Morris Traversal** allows traversal in **O(1) extra space**,
by **temporarily modifying the tree** and restoring it later.

---

## 2️⃣ Preorder traversal refresher (very important)

**Preorder order = Root → Left → Right**

Key rule:

> **The moment you see a node for the FIRST time, you VISIT it**

This rule is the anchor for Morris Preorder.

---

## 3️⃣ Core Idea of Morris Traversal (Mental Model)

> “If recursion uses a stack to come back to a node,
> Morris traversal creates a temporary pointer to come back.”

### How?

* For each node:

  * If it has a **left subtree**
  * Find its **predecessor** (rightmost node in left subtree)
  * Create a **temporary link** from predecessor → current node

This temporary link replaces the recursion stack.

---

## 4️⃣ What is a “Predecessor”?

For a node `cur`:

* **Preorder predecessor does NOT matter**
* We use **inorder predecessor** because:

  * It’s the **rightmost node in left subtree**
  * It is the **last node visited before returning to cur**

### Finding predecessor:

```java
TreeNode pred = cur.left;
while (pred.right != null && pred.right != cur) {
    pred = pred.right;
}
```

---

## 5️⃣ Morris Preorder — Decision Tree (MEMORIZE THIS)

At **every node `cur`**, ask:

### ❓ Does `cur.left == null`?

### Case 1️⃣: `left == null`

* No left subtree
* Visit node immediately (preorder rule)
* Move right

```java
visit(cur)
cur = cur.right
```

---

### Case 2️⃣: `left != null`

Now find predecessor.

#### Case 2A️⃣: predecessor.right == null

👉 First time visiting this node

* **VISIT the node** (important difference from inorder)
* Create thread:

  ```
  predecessor.right = cur
  ```
* Move left

```java
visit(cur)
pred.right = cur
cur = cur.left
```

---

#### Case 2B️⃣: predecessor.right == cur

👉 Second time we reached this node (coming back)

* Remove thread
* Move right

```java
pred.right = null
cur = cur.right
```

---

## 6️⃣ WHY preorder visit happens EARLY

### Compare with Inorder Morris

| Traversal | When do we visit root? |
| --------- | ---------------------- |
| Inorder   | After left subtree     |
| Preorder  | Before left subtree    |

That’s **the only difference**.

💡 **Mnemonic**

> *“Preorder visits BEFORE creating the thread”*

---

## 7️⃣ Full Code (with inline explanation)

```java
public List<Integer> preorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    TreeNode cur = root;

    while (cur != null) {

        // CASE 1: No left child
        if (cur.left == null) {
            result.add(cur.val);      // Visit root
            cur = cur.right;          // Move right
        }

        // CASE 2: Left child exists
        else {
            // Find predecessor (rightmost of left subtree)
            TreeNode pred = cur.left;
            while (pred.right != null && pred.right != cur) {
                pred = pred.right;
            }

            // CASE 2A: First visit
            if (pred.right == null) {
                result.add(cur.val);  // Visit root (preorder)
                pred.right = cur;     // Create thread
                cur = cur.left;       // Move left
            }

            // CASE 2B: Second visit
            else {
                pred.right = null;    // Remove thread
                cur = cur.right;      // Move right
            }
        }
    }

    return result;
}
```

---

## 8️⃣ Dry Run (Must Memorize Once)

Tree:

```
       1
      / \
     2   3
    / \
   4   5
```

### Step-by-step:

1. `cur = 1`, left exists

   * predecessor = 5
   * visit 1
   * create thread 5 → 1
   * go left

2. `cur = 2`, left exists

   * predecessor = 4
   * visit 2
   * create thread 4 → 2
   * go left

3. `cur = 4`, left null

   * visit 4
   * go right (thread → 2)

4. `cur = 2` again

   * predecessor.right == cur
   * remove thread
   * go right

5. `cur = 5`, left null

   * visit 5
   * go right (thread → 1)

6. `cur = 1` again

   * remove thread
   * go right

7. `cur = 3`, left null

   * visit 3
   * done

### Output:

```
[1, 2, 4, 5, 3]
```

---

## 9️⃣ Space & Time Complexity (Interview Gold)

* **Time:** O(n)

  * Each edge is visited **at most twice**
* **Space:** O(1)

  * No recursion
  * No stack

---

## 🔑 Memory Hooks (Very Important)

Use these to **memorize forever**:

1. **“Preorder = visit when you SEE the node”**
2. **“Thread is only for coming back”**
3. **“Create thread → go left”**
4. **“Remove thread → go right”**
5. **“Visit BEFORE thread creation”** (difference from inorder)

---

## ⚠️ Common Mistakes

❌ Visiting node after left subtree → that becomes inorder
❌ Forgetting to remove thread → infinite loop
❌ Not checking `pred.right != cur` → wrong traversal

---

## 🧠 One-Line Interview Explanation

> “Morris Preorder traversal achieves O(1) space by creating temporary threads to a node’s inorder predecessor, visiting the node before going left, and restoring the tree structure after traversal.”

