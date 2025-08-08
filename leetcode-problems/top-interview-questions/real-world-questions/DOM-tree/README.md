### 1. 🧩 **Find Nodes Matching a Phrase in Mixed DOM Content**
[](#1--split-phrase-match-across-dom-tree)
**Prompt:**

> You’re given a tree representing a simplified DOM. Each node can be a text node or element node. Given a phrase like `"hello world"`, return the minimal set of nodes that together contain this phrase in order. Partial matches and split words across nodes are allowed.

**Follow-up:** Optimize for large DOM trees.

👉 Similar to what you faced.

---

### 2. 🌳 **Search Subtree Matching Given HTML Fragment**
[](#2--match-subtree-by-html-fragment)
**Prompt:**

> Given a DOM tree and a small subtree representing an HTML snippet (e.g., `<div><p>Hello</p></div>`), check if any subtree in the DOM matches it **exactly in structure and content**.

**Skills:** Tree comparison, structural recursion

---

### 3. 🔗 **Concatenated Paths in File System**

[](#4--full-path-match-in-file-system-tree)

**Prompt:**

> You have a directory tree where each file or directory is a node with a name. Find all paths where the full path string (from root to leaf) contains a given search phrase.

**Follow-up:** Return paths where `searchTerm` is split across directory names.

---

### 4. 🔍 **Search Token Across JSON Tree**

**Prompt:**

> Given a nested JSON object (tree), find all nodes that contain the **search string** (case-insensitive), even if the match is split across nested fields.

**Follow-up:** Match should consider field names and values.

---

### 5. 🧠 **Regex Match on Flattened DOM**

[](#6--regex-match-across-dom-nodes)

**Prompt:**

> Flatten a DOM tree and apply a regex pattern like `"is.*code"` to the combined content. Return nodes contributing to the match.

**Hint:** Similar to your original, but now generalized for regex.

---

### 6. 🧬 **Subsequence Match in XML Tree**

**Prompt:**

> Given an XML-like tree, determine whether a given word like `"machine"` is a **subsequence** of the concatenated leaf text content. Return boolean or matched nodes.

**Skills:** DFS + subsequence + character tracking

---

### 7. 🧭 **Build Index from Tree for Fast Phrase Search**

**Prompt:**

> Given a DOM tree, preprocess it to allow **fast search** for any phrase later (e.g., "code is"). You can index at build time. How would you do it?

**Skills:** Indexing, search optimization, suffix trees, memory vs speed

---

### 8. 🪢 **Cross-Element Text Replacement**

**Prompt:**

> Replace the phrase `"old phrase"` with `"new phrase"` in a tree of text nodes, where the phrase can be split across nodes. Merge/split nodes if needed.

**Skills:** Tree rewrite, DOM manipulation

---

### 9. 📄 **Generate XPath to Nodes Matching Word**

**Prompt:**

> Given a DOM tree and a word, return the XPath(s) to all text nodes that contain or contribute to that word (even if split across children).

**Skills:** Path tracking, text search, XPath building

---

### 10. 🧩 **Highlight All Matches in Tree**

**Prompt:**

> Given a DOM-like tree and a search term, wrap all matching parts in `<mark>` tags — even if split across nodes.

**Follow-up:** How to do this without rebuilding the full DOM?

---

## ✨ Bonus: Practice Enhancement

For each of these, practice:

* Brute force then optimize (e.g., KMP, suffix arrays, tries)
* Time and space complexity
* Edge cases: empty nodes, partially overlapping text, deep nesting
* Follow-up tradeoffs (streaming vs pre-indexing, memory usage, etc.)

---


### 1. 🧩 **Split Phrase Match Across DOM Tree**

> You’re given a simplified DOM tree consisting of `TextNode`s and `ElementNode`s, each having zero or more children. Implement a function to search for a phrase like `"is code"` where:
>
> * The phrase can be split across multiple text nodes
> * Partial matches are allowed
> * Return all `TextNode`s that contribute to a match
>
> Use Java and explain time and space complexities. Then optimize using KMP or other techniques.

---

### 2. 🌳 **Match Subtree by HTML Fragment**

> Implement a function that, given a root DOM tree and a small HTML snippet as a tree fragment, checks whether an identical subtree exists anywhere in the DOM.
>
> Structure, tags, and text must match exactly. Return the root node of the matched subtree if found.

---

### 3. 🔎 **Search Phrase in Flattened DOM Content**

> Flatten a DOM tree (with nested elements and text) and search for a phrase like `"machine learning"`. The phrase may span multiple nodes. Return the minimal set of text nodes contributing to this phrase, maintaining order.
>
> Implement both a brute-force and an optimized version using KMP.

---

### 4. 🔗 **Full Path Match in File System Tree**

> You’re given a tree structure representing a file system. Each node has a name, and children. Return all paths where the **full concatenated path string** (e.g., `"root/documents/reports"`) contains a given phrase.
>
> The phrase may be split across directory names. Implement in Java.

---

### 5. 🧬 **Subsequence Match in Text Node Tree**

> Given a tree where leaves contain strings and internal nodes represent HTML elements, write a function to check whether a given string is a **subsequence** of the concatenated leaf text content (in DFS order).
>
> For example, `"machine"` should return true if characters appear in order across leaves.

---

### 6. 🧠 **Regex Match Across DOM Nodes**

> You’re given a DOM tree. Implement a function to apply a regex like `"is.*code"` across the concatenated text content (in DFS order). Return all text nodes that contribute to any matching substring.

---

### 7. 🧭 **Index DOM Tree for Fast Search**

> You’re building a search engine for a DOM tree. Preprocess the tree to allow fast search for phrases like `"machine learning"` later. The phrase might span multiple nodes.
>
> Design the data structure and implement the search. Discuss time/space tradeoffs.

---

### 8. 🪢 **Cross-Node Text Replacement**

> Implement a function that replaces a phrase like `"old value"` with `"new value"` in a DOM tree, even if `"old value"` is spread across multiple text nodes.
>
> Handle edge cases like splitting and merging text nodes. Maintain correct node order and structure.

---

### 9. 📍 **Generate XPath for Matching Nodes**

> Given a DOM tree and a phrase, return the XPath(s) to all nodes (text or element) that contain or contribute to matching that phrase.
>
> The phrase may be split across multiple nodes. Implement both naive and optimized versions.

---

### 10. 🖍️ **Highlight Matches in DOM Tree**

> Write a function that takes a DOM tree and a phrase like `"data science"`, and wraps every match in `<mark>` tags, even when the phrase is split across multiple nodes.
>
> Output should be the modified DOM tree structure. Implement in JavaScript or Java.

---

## 🛠️ Usage Tips

* Feed these prompts **one at a time** to simulate solving them under pressure.
* Ask the LLM to "critique and optimize the solution."
* Add follow-ups like:

  * “Now optimize it using a streaming approach.”
  * “How would you scale this if the tree had millions of nodes?”
  * “Explain your choice of data structure.”
