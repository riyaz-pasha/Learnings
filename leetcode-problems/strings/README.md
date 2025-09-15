# 🚀 Must-Know String Algorithms

## ✅ **Core Concepts (Master These First)**

### 1. **String Traversal and Manipulation**

* Reversal (`reverse()`, 2-pointer)
* Palindrome check (naive, 2-pointer)
* Removing duplicates / extra spaces / special chars
* Example:
  * *Reverse Words in a String*
  * *Valid Palindrome*

* [151-reverse-words](./151-reverse-words.java) [Reverse Words in a String](https://leetcode.com/problems/reverse-words-in-a-string/)
* [125-valid-palindrome](./125-valid-palindrome.java) [Valid Palindrome](https://leetcode.com/problems/valid-palindrome/)
* [443-string-compression](./443-string-compression.java) [String Compression](https://leetcode.com/problems/string-compression/)


### 2. **String Matching (Pattern Search)**

* **Naive search**
* **KMP (Knuth-Morris-Pratt)** → O(n + m)
* **Rabin-Karp** → Rolling hash (O(n))
* **Z-algorithm** → O(n)
* Example:

  * *Implement strStr()*
  * *Find All Anagrams in a String*
  * *Detect Repeated Substrings*

* [Implement strStr()](https://leetcode.com/problems/implement-strstr/)
* [Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) [Code](./28-find-the-index-of-the-first-occurrence-in-a-string.java)
* [Repeated DNA Sequences](https://leetcode.com/problems/repeated-dna-sequences/)

---

## 🔁 **Sliding Window + Hashing (Very Common)**

### 3. **Sliding Window**

* Fixed-size and variable-size windows
* Frequency maps with HashMap or array
* Example:

  * *Longest Substring Without Repeating Characters*
  * *Minimum Window Substring*
  * *Permutation in String*

* [Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/)
* [Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/)
* [Permutation in String](https://leetcode.com/problems/permutation-in-string/)
* [Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/)


### 4. **Two Pointers on Strings**

* Compare or shrink/grow window
* Used for palindrome, anagram checking, or compressing strings
* Example:

  * *Valid Palindrome II*
  * *Longest Palindromic Substring* (with center expansion)

* [Valid Palindrome II](https://leetcode.com/problems/valid-palindrome-ii/)
* [Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/)


---

## 📊 **Frequency Counting / Hash Maps**

### 5. **Anagrams**

* Grouping anagrams using sorted string or char count tuple
* Example:

  * *Group Anagrams*
  * *Valid Anagram*
  * *Find All Anagrams in a String*

* [Group Anagrams](https://leetcode.com/problems/group-anagrams/)
* [Valid Anagram](https://leetcode.com/problems/valid-anagram/)

---

## 🔄 **Dynamic Programming on Strings**

### 6. **DP Problems**

* **Longest Common Subsequence (LCS)**
* **Longest Palindromic Subsequence**
* **Longest Palindromic Substring**
* **Edit Distance (Levenshtein Distance)**
* **Regular Expression Matching** (with `.` and `*`)
* **Wildcard Matching** (with `?` and `*`)
* Example:

  * *Edit Distance*
  * *Distinct Subsequences*
  * *Regex Matching*

* [Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/)
* [Longest Palindromic Subsequence](https://leetcode.com/problems/longest-palindromic-subsequence/)
* [Edit Distance](https://leetcode.com/problems/edit-distance/)
* [Distinct Subsequences](https://leetcode.com/problems/distinct-subsequences/)
* [Regular Expression Matching](https://leetcode.com/problems/regular-expression-matching/)
* [Wildcard Matching](https://leetcode.com/problems/wildcard-matching/)

---

## 🧠 **Tries / Prefix Trees**

### 7. **Trie Usage**

* Insert/Search/Delete strings in prefix tree
* Autocomplete, prefix search
* Word search on 2D board
* Example:

  * *Implement Trie*
  * *Word Search II*
  * *Replace Words*

* [Implement Trie](https://leetcode.com/problems/implement-trie-prefix-tree/)
* [Replace Words](https://leetcode.com/problems/replace-words/)
* [Word Search II](https://leetcode.com/problems/word-search-ii/)

---

## 💥 **Advanced Algorithms / Techniques**

### 8. **Suffix Arrays and LCP Arrays**

* Used in:

  * Substring matching
  * Number of distinct substrings
  * Longest repeated substring
  * Often used with **Kasai’s Algorithm** or **SA-IS**

  > Rare but impressive if you know this.

* [Longest Duplicate Substring](https://leetcode.com/problems/longest-duplicate-substring/)

### 9. **Manacher’s Algorithm** (O(n) Palindrome)

* Efficient way to find **longest palindromic substring** in linear time

* [Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/)

---

## 🧩 **Combinatorics / Recursion / Backtracking**

### 10. **Backtracking on Strings**

* Generate permutations, combinations
* Restore IP addresses
* Palindrome partitioning
* Example:

  * *Letter Combinations of a Phone Number*
  * *Generate Parentheses*
  * *Word Break II*

* [Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/)
* [Generate Parentheses](https://leetcode.com/problems/generate-parentheses/)
* [Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/)

### 11. **Word Break Problems**

* Using DP + Trie + backtracking
* Important variations in interviews
* Example:

  * *Word Break*
  * *Word Break II*

* [Word Break](https://leetcode.com/problems/word-break/)
* [Word Break II](https://leetcode.com/problems/word-break-ii/)

---

## 🔍 **Miscellaneous But Useful**

### 12. **Run Length Encoding / Compression**

* *String Compression* (Leetcode)
* *Count and Say*

* [String Compression](https://leetcode.com/problems/string-compression/)
* [Count and Say](https://leetcode.com/problems/count-and-say/)

### 13. **Rolling Hash + Set**

* Detecting repeated substrings (e.g., of length k)
* Example:

  * *Repeated DNA Sequences*
  
* [Repeated DNA Sequences](https://leetcode.com/problems/repeated-dna-sequences/)

---

## 🧠 Pattern Summary by Problem Type

| Type             | Common Patterns                    |
| ---------------- | ---------------------------------- |
| Substring        | Sliding Window, HashSet, 2-pointer |
| Subsequence      | DP (memo/tabulation)               |
| Matching         | KMP, Trie, Regex DP                |
| Palindromes      | Two Pointers, DP, Manacher’s       |
| Grouping         | HashMaps, Sorting, Tuples          |
| Compression      | Stack, 2-pointer                   |
| Dictionary-based | Trie, Backtracking, DP             |

---
