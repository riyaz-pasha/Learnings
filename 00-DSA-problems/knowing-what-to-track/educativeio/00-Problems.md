# Knowing What to Track — Final DSA Roadmap

For your DSA preparation, I recommend organizing these 32 problems by the actual reasoning skill they teach, not simply by whether their solutions contain counters or a `HashMap`.

We'll use three classifications:

1. Core pattern: Problems where identifying and maintaining the right state is a central part of the solution.
2. Hybrid problems: Tracking is important, but another pattern or mathematical technique must be learned as well.
3. Separate patterns: Problems better solved and practiced under another algorithmic category.

I'll also give you the recommended order, what to track, the main insight, and the prerequisite knowledge for each problem. The code examples, where useful, will use modern Java.

## Part 1 — Core pattern: solve these 16 first

The sequence progresses from basic frequency counting to more sophisticated state tracking and data-structure design.

### Tier 1 — Foundations: frequency counting

Learn how to represent counts, consume counts, and derive answers from counts before moving to more complex problems.

[Palindrome in Scrabble - Dodona](https://images.openai.com/static-rsc-4/Etr9N94SmVgi_DzhXK8YqHFKNkme0esXQ8QBDaWVK-ruNJ_vcJD_ka6WEJ2LlGmKXU9aZ3V8tpKZwcI2BV7agce1Q-POORh5afUZknKKIyNMa1nwQqacZBWRxBfXtXkSHR8LLhNJzG-HD9XIKFJ7hDbW-cDdlnwtI84Mqax8T_E?purpose=inline)

### 1. Valid Anagram

Easy · Frequency counting

Track: The frequency of each character.

Key insight: Two strings are anagrams if their character frequencies are identical.

Recommended approach: For lowercase English letters, use an `int[26]`. Increment for the first string and decrement for the second. Every count must end at zero.

Complexity: \\(O(n)\\) time, \\(O(1)\\) auxiliary space for a fixed alphabet.

Why first: It teaches the basic frequency-array template.

[Vector Ransom Note #1- Cut Paper Letters, Numbers, Symbols](https://images.openai.com/static-rsc-4/CV-cERyaiHXLukoXE15IqdxSMGhut49elXFUfrM48Ekt43PN3nmabQ3eRxrzM7OVZKxxdBcweGP39KSfMnlmhr-IajUqbKuEyYsl_QCKC3Ru6d7yRNKxKt894VLqPa4UtTTBAAnerfQOMiH48zq0DoFptLa_rNbkl3SgroeAkYkrtr8NRgi7rLSkAneavZDw?purpose=inline)

### 2. Ransom Note

Easy · Frequency consumption

Track: Available character counts from the magazine.

Key insight: Each character in the note consumes one available occurrence. If a required count is unavailable, the construction is impossible.

Recommended approach: Build the magazine's frequency array, then decrement as you process the note.

Complexity: \\(O(m+n)\\) time, \\(O(1)\\) auxiliary space for a fixed alphabet.

Why second: It builds directly on Valid Anagram, but introduces the idea of consuming tracked resources.

[JNF LETRA   R  INOX | Ferreteria Maderas 3C](https://images.openai.com/static-rsc-4/OlFpm_MuhEPIHtSTdcw3a2P8rE7PdDppVXgbQebWI6hEzMiVylhl-rMELriohHv8ocGucV_yHg7yFlSIrE8bDgjfGT-QK7APLhhgMMeAM9A8pY__OTOWxjhhPKhjFE4_ERdufFEGDxVrgT0eM8UF-bpUiDGxCkq-XA-pB1NgBWY?purpose=inline)

### 3. First Unique Character in a String

Easy · Frequency plus order

Track: The frequency of each character.

Key insight: Counting tells you which characters are unique, but a second scan preserves the original string order and finds the first one.

Recommended approach: Count in the first pass, scan the original string in the second pass.

Complexity: \\(O(n)\\) time, \\(O(1)\\) space for a fixed alphabet.

Why third: It teaches that the frequency table and the original sequence can serve different purposes.

[ΠΑΙΧΝΊΔΙΑ ΛΈΞΕΩΝ 🗒️ - Παίξτε Δωρεάν Online! | Poki (ποκι)](https://images.openai.com/static-rsc-4/BKmsRZwfh5A63zh85p5acdRdUM828i_km1WTO-acTeSn1P1XnzKUxehi785aio8VLbZdnDatmp70Q3P64bBLMW64CRyhARAg8fZgJLe9602wRyYD3q2wRxZRscRSnFPhRgSDelWrtcB2hJsiKFGMbkBxu8QjbS6Rl-3VQS2qcrg?purpose=inline)

### 4. Find Words That Can Be Formed by Characters

Easy · Reusable frequency template

Track: Available character frequencies and each candidate word's requirements.

Key insight: A word is formable only when every required character count is at most the available count.

Recommended approach: Build the available frequency array once, check each word independently, and add the lengths of valid words.

Complexity: \\(O(C+W)\\) time, where \\(C\\) is the total number of characters processed and \\(W\\) is the number of words; fixed-alphabet auxiliary space.

Why fourth: It applies resource counting repeatedly without allowing one candidate word to consume resources needed by another.

[Memory Games: Numbers 1-54: 12 Pages: Free Teaching Resources](https://images.openai.com/static-rsc-4/Z7grzu55Ou-1JiWRHZyu1Rl-pP6F5IjIfiyi91I_SuJTx4Y7HM_iPLdmab-NF_XOrxd_6AiEOFuEz8MLIQNxa0U691lIDlj1UR4Yf4RN0hIK23N9xf7YdqbrUxiFGMEzbVOTNzzpa5Pz5zJ0QcQZPFcJT7it0NFmz-_h8LgAGIM?purpose=inline)

### 5. Number of Good Pairs

Easy · Incremental counting

Track: How many times each value has appeared so far.

Key insight: If the current value has already appeared \\(c\\) times, it creates exactly \\(c\\) new good pairs.

Recommended approach: Add the existing frequency to the answer, then increment the frequency.

Complexity: Expected \\(O(n)\\) time and \\(O(u)\\) space with a `HashMap`, where \\(u\\) is the number of distinct values.

Why fifth: It teaches the important rule that the answer can often be updated directly from the state rather than computed afterward.

[Anagrams of Palindromic Words - Word Checker](https://images.openai.com/static-rsc-4/6FiRadnHoRTwbxxqlaISDgTFFnk03uohh8lb2ClURMWMNer833qbza3K5pjCX2EwUOLUsL0IReC8tvN3f07zfkncaPmikobuXyyXHUQ2TpsPpAkw6PdCxtCWqNG7JvnZzy1pPNGxbbZWWez2FVBrYVusMO5G50ABMaxjOPDDIJA?purpose=inline)

### 6. Palindrome Permutation

Easy · Frequency parity

Track: Which characters have odd frequencies.

Key insight: A string can be rearranged into a palindrome if at most one character has an odd count.

Recommended approach: Count frequencies and count the odd entries. Alternatively, toggle character parity with a bitmask for a restricted alphabet.

Complexity: \\(O(n)\\) time and \\(O(1)\\) space for a fixed alphabet.

Why sixth: It teaches how to extract a mathematical property from frequency counts without needing the exact counts themselves.

### Tier 2 — Frequency plus a second step

Now you understand counting. These problems add sorting, canonical keys, complement lookups, greedy decisions, or comparisons between categories.

| Order | Problem                                                       | What to track                           | Additional concept           | Why here                                                                                                    |
| ----- | ------------------------------------------------------------- | --------------------------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------- |
| 7     | Sort Array by Increasing Frequency — Easy/Medium              | Frequency of every value                | Custom sorting               | Count first, then sort by ascending frequency and descending value for ties.                                |
| 8     | Group Anagrams — Medium                                       | A canonical representation of each word | Hashing/grouping             | Map words with equivalent character composition to the same key.                                            |
| 9     | Longest Palindrome by Concatenating Two-Letter Words — Medium | Count of each two-letter word           | Pairing and symmetry         | Pair a word with its reverse; handle symmetric words and the center carefully.                              |
| 10    | Pairs of Songs With Total Durations Divisible by 60 — Medium  | Previously seen remainders modulo 60    | Complement lookup            | A remainder \\(r\\) pairs with \\((60-r)\bmod60\\).                                                         |
| 11    | Minimum Number of Pushes to Type Word II — Medium             | Character frequencies                   | Greedy optimization          | Sort frequencies descending and assign the most frequent letters to the cheapest key positions.             |
| 12    | Rank Teams by Votes — Medium                                  | Vote counts for each team at every rank | Lexicographic comparison     | Compare first-place counts, then second-place counts, and so on; use alphabetical order for a complete tie. |
| 13    | Minimum Operations to Write the Letter Y on a Grid — Medium   | Digit counts inside and outside the Y   | Enumeration and optimization | Try every pair of distinct target digits and calculate the changes from the counts.                         |

Important implementation notes

- For problem 7, the tie-breaker is value in descending order, not ascending order.
- For problem 8, a sorted string or a 26-character frequency signature can be the hash key. A frequency signature is linear in word length for a fixed alphabet; sorting each word costs \\(O(L\log L)\\).
- For problem 9, reverse-word counts must be handled without double-counting. Words such as `"gg"` can pair with themselves, and at most one unpaired symmetric word can occupy the center.
- For problem 10, when the remainder is zero, its complement is also zero; do not treat it as 60.
- For problem 13, the two target digits must be different because the Y region and the non-Y region require different digits.

### Tier 3 — Advanced state tracking and data-structure design

These are the final three core problems. They require you to design what information to store so that operations or decisions can be performed efficiently.

[Tic tac toe game Icons, Logos, Symbols – Free Download PNG, SVG](https://images.openai.com/static-rsc-4/5MgjMmTkw9TE2wWpJ1cGJNLduQmr4NDrlG4bjW_6WpSJT4akeuY6V4QcJ3W-hz3mSlnWQKZ4ydMGxexoR0dVeBuEPgk2ImiJDt48dxWM9auyIp5eEpDsIIXlQSKhK5q1allX4uQ4ML4Dpij1QuHpJrvhZYgacRB9pLaDVnlecwQ?purpose=inline)

### 14. Design Tic-Tac-Toe

Medium · State maintenance

Track: Row totals, column totals, and the two diagonal totals, using signed contributions for the two players.

Key insight: You don't need to inspect the entire board after every move. Maintain enough state to detect a winning row, column, or diagonal immediately.

Complexity: \\(O(1)\\) time per move and \\(O(n)\\) space for an \\(n\times n\\) board.

Why here: This teaches you to design a compact state representation for repeated operations.

[Wooden block stacking, Financial and Business growth concept using as background](https://images.openai.com/static-rsc-4/QWoghlNjV5uuP_77VoYn_GpD9CZNEcGNFGRAw9VcvodFp1Hv3NRj30C3igIZ_msKV9lD4q08ZlqIchH_MBLvXneVZhrVEB6NlrvfazBfn7GmCxlcffsTHkHvQ_64pIumrKn4dbh1zaD0FP11wq7wqQ1j2Zs7oiskaAqVcEfWAiFAReifB8RDkzu6EWnL05fH?purpose=inline)

### 15. Divide Array Into Increasing Sequences

Hard · Frequency-based feasibility

Track: The maximum frequency of any value.

Key insight: A strictly increasing sequence cannot contain the same value twice. If the maximum allowed number of sequences is \\(k\\), the array is feasible exactly when:

\\[ \boxed{\text{maxFreq}\leq k} \\]

Recommended approach: Count frequencies and keep the maximum. You do not need to construct the sequences.

Complexity: \\(O(n+u)\\) time and \\(O(u)\\) space with a frequency map, where \\(u\\) is the number of distinct values.

Why here: It shows how one carefully chosen statistic can answer a seemingly complicated construction question.

[Maximum Frequency Stack](https://images.openai.com/static-rsc-4/QnGGAAeGAY7j9IpZzeLkgoKTMDZ7yelXbFd-1Fc_TwyvS3eX7cxqpzJfm0Vnv3W2-MBQoTNabALyVn-EpIynAsO11dNQpRtcewDrowdd2WE7gdW01cpQuQjpgScixKvTwMNFe2veTwt-omHM3J-AAWPDqvkdfFvzw3by58kCcGA?purpose=inline)

### 16. Maximum Frequency Stack

Hard · Multiple coordinated states

Track: The frequency of each value, the maximum frequency, and a stack of values for each frequency level.

Key insight: Pop the most frequent value; if multiple values share that frequency, pop the one most recently pushed among them.

Recommended approach: Use a frequency map and a map from frequency to stack. Maintain the current maximum frequency.

Complexity: Expected \\(O(1)\\) per push and pop, with \\(O(n)\\) total space.

Why last: It is the strongest exercise in designing multiple pieces of state that work together.

A classification note: Design Tic-Tac-Toe and Maximum Frequency Stack are state-maintenance design problems, not pure frequency-table problems. I am retaining them in this core group because deciding exactly what to track is central to both solutions.

## Part 2 — Hybrid problems: solve after their prerequisites

These two problems use tracking, but another technique is essential to understanding the optimal solution.

## 17. Find All Anagrams in a String

Medium · Frequency counting + sliding window

What to track: Character frequencies in a fixed-size window of the larger string.

Key insight: An anagram of a pattern must have the same length and the same character frequencies. Instead of recounting every substring, update the frequency state as the window moves.

Learning order: Valid Anagram → sliding-window fundamentals → Find All Anagrams in a String.

Complexity: \\(O(n+m)\\) time and \\(O(1)\\) auxiliary space for a fixed alphabet, where \\(n\\) and \\(m\\) are the two string lengths.

Why not in the core 16? The sliding-window technique is a major part of the solution. Frequency tracking alone is insufficient.

## 18. Count Anagrams

Hard · Frequency counting + combinatorics

What to track: Character frequencies within each word.

Key insight: The number of distinct permutations of a word of length \\(n\\), with character frequencies \\(c_1,c_2,\ldots,c_j\\), is

\\[ \frac{n!}{c_1!c_2!\cdots c_j!} \\]

For the standard problem, combine the counts across words using the required modular arithmetic.

Learning order: Frequency counting → factorials and permutations → modular arithmetic and modular inverses.

Complexity: Linear in the total input length, assuming factorial and inverse-factorial values are precomputed up to the maximum word length.

Why not in the core 16? The central challenge is counting permutations mathematically, not just maintaining character frequencies.

## Part 3 — The remaining 14 problems: learn them under their primary patterns

These problems may track useful information, but the principal algorithmic idea is different. Keep them in their appropriate DSA study tracks.

### A. Basic traversal and simulation

| Problem                                          | Primary pattern                         | What the solution tracks                                        | Recommended approach                                                                     |
| ------------------------------------------------ | --------------------------------------- | --------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| 19. Max Consecutive Ones — Easy                  | Array traversal / running streak        | Current streak and maximum streak                               | Increment the current streak for `1`; reset it on `0`; update the maximum.               |
| 20. Count and Say — Medium                       | String simulation / run-length encoding | Consecutive equal characters in the current term                | Build each new term by describing runs in the previous term.                             |
| 21. Concatenation of Array — Easy                | Array construction                      | Output position                                                 | Copy the original array twice into a result array.                                       |
| 22. Zigzag Conversion — Medium                   | String simulation                       | Current row and movement direction                              | Move down and up through the rows, appending each character to its row.                  |
| 23. Count Hills and Valleys in an Array — Medium | Linear scan / local comparisons         | Previous distinct value, current value, and next distinct value | Ignore duplicate neighbors and compare distinct adjacent values to detect local extrema. |

Suggested order: Concatenation of Array → Max Consecutive Ones → Count Hills and Valleys → Count and Say → Zigzag Conversion.

### B. Prefix sums and range-update techniques

| Problem                                  | Primary pattern                        | What the solution tracks                    | Recommended approach                                                                                                                                                |
| ---------------------------------------- | -------------------------------------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 24. Find Pivot Index — Medium            | Prefix sums                            | Left sum and total sum                      | A pivot satisfies `leftSum == totalSum - leftSum - nums[i]`.                                                                                                        |
| 25. Zero Array Transformation I — Medium | Difference array / prefix accumulation | Range-update boundaries and active coverage | Apply range updates to a difference array, then compute coverage with a prefix sum and check whether each element can be reduced to zero under the problem's rules. |

Suggested order: Find Pivot Index → Zero Array Transformation I.

### C. Two pointers and mismatch analysis

| Problem                                                    | Primary pattern                       | What the solution tracks                    | Recommended approach                                                                                      |
| ---------------------------------------------------------- | ------------------------------------- | ------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| 26. Check if One String Swap Can Make Strings Equal — Easy | String comparison / mismatch analysis | The mismatch positions and their characters | Identical strings are valid; otherwise there must be exactly two mismatches whose characters cross-match. |
| 27. Find the Celebrity — Medium                            | Two-pointer candidate elimination     | A single possible celebrity                 | Eliminate one candidate per comparison, then verify the survivor against everyone else.                   |

Suggested order: Check if One String Swap Can Make Strings Equal → Find the Celebrity.

### D. Greedy, extrema, and monotonic reasoning

| Problem                                            | Primary pattern                     | What the solution tracks                                                    | Recommended approach                                                                                                                   |
| -------------------------------------------------- | ----------------------------------- | --------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| 28. Maximum Product of Three Numbers — Medium      | Greedy / extrema tracking           | Three largest values and two smallest values                                | Compare the product of the three largest values with the product of the largest value and two smallest values.                         |
| 29. Shortest Unsorted Continuous Subarray — Medium | Array ordering / monotonic stack    | Misplaced boundaries, prefix maxima and suffix minima, or a monotonic stack | Find the shortest interval whose sorting makes the entire array sorted.                                                                |
| 30. Fruits Into Baskets II — Medium                | Greedy simulation / data structures | Used baskets and their capacities                                           | Process fruits in order and select the first unused suitable basket. A segment tree can optimize finding the leftmost suitable basket. |

Suggested order: Maximum Product of Three Numbers → Shortest Unsorted Continuous Subarray → Fruits Into Baskets II.

### E. Bit manipulation and compact state sets

| Problem                               | Primary pattern                        | What the solution tracks                                    | Recommended approach                                                                                                      |
| ------------------------------------- | -------------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| 31. Count Binary Substrings — Medium  | Run-length encoding                    | Lengths of adjacent runs of zeros and ones                  | Add `min(previousRunLength, currentRunLength)` whenever a new run is completed.                                           |
| 32. Bitwise ORs of Subarrays — Medium | Dynamic programming / bit manipulation | Distinct OR values of subarrays ending at the current index | Extend each previous OR state with the current value, add the current value itself, and deduplicate the resulting states. |

Suggested order: Count Binary Substrings → Bitwise ORs of Subarrays.

## Part 4 — Your final master checklist

Use this as the definitive order for your study sessions.

## My DSA progress

### 0/32

Tier 1 — Frequency foundations

Valid Anagram

Ransom Note

First Unique Character in a String

Find Words That Can Be Formed by Characters

Number of Good Pairs

Palindrome Permutation

Tier 2 — Frequency plus another step

Sort Array by Increasing Frequency

Group Anagrams

Longest Palindrome by Concatenating Two-Letter Words

Pairs of Songs With Total Durations Divisible by 60

Minimum Number of Pushes to Type Word II

Rank Teams by Votes

Minimum Operations to Write the Letter Y on a Grid

Tier 3 — Advanced state maintenance

Design Tic-Tac-Toe

Divide Array Into Increasing Sequences

Maximum Frequency Stack

Hybrid problems

Find All Anagrams in a String

Count Anagrams

Separate pattern — Traversal and simulation

Concatenation of Array

Max Consecutive Ones

Count Hills and Valleys in an Array

Count and Say

Zigzag Conversion

Separate pattern — Prefix sums and range updates

Find Pivot Index

Zero Array Transformation I

Separate pattern — Mismatch and elimination

Check if One String Swap Can Make Strings Equal

Find the Celebrity

Separate pattern — Greedy and ordering

Maximum Product of Three Numbers

Shortest Unsorted Continuous Subarray

Fruits Into Baskets II

Separate pattern — Runs and bit manipulation

Count Binary Substrings

Bitwise ORs of Subarrays

ResetCopy checklist

## Part 5 — When should you move to the next problem?

Don't move forward just because you have accepted a solution. For each of the first 16 problems, make sure you can answer these questions without looking at your notes:

1. What information am I tracking, and why is it sufficient?
2. Why is this information better than repeatedly scanning the input?
3. When should I update the state relative to the current element?
4. What invariant establishes correctness?
5. What is the time and space complexity?
6. Which edge case could break a careless implementation?

For the advanced problems, add one more question: Could I maintain less state and still answer every query correctly?

That last question is especially valuable for interview problems involving data-structure design.

My recommendation is to complete the 16 core problems in order, then solve the two hybrids after learning their prerequisites. Keep the other 14 in your existing pattern-specific study tracks. This way, you learn a reusable problem-solving technique instead of trying to fit every problem into one category.
