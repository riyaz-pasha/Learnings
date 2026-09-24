# What Did We Learn?

## What have we accomplished?

The problems we solved while building out our C++ language compiler's features are the same patterns that show up again and again in interviews at top-tier companies. Now that we've built them once in a real-world context, we can recognize the underlying pattern quickly wherever it resurfaces.

Here's how each compiler feature maps to its real interview pattern:

| Language Compiler Feature | Underlying Interview Pattern |
|---|---|
| Feature #1: Remove Comments | Comment stripping / stateful string scan (LeetCode 722 — Remove Comments) |
| Feature #2: Evaluate the Arithmetic Expression | Stack-based expression evaluation with parentheses (LeetCode 224 — Basic Calculator) |
| Feature #3: Loop Unrolling | Nested repeat-count decoding with a stack (LeetCode 394 — Decode String) |
| Feature #4: Optimization by Replacement | Right-to-left indexed string replacement (LeetCode 833 — Find and Replace in String) |
| Feature #5: Compilation Step Failure | Binary search on a sorted boolean predicate (LeetCode 278 — First Bad Version) |
| Feature #6: Most Common Token | Normalize-and-count word frequency (LeetCode 819 — Most Common Word) |
| Feature #7: Exponentiation for Mobile Devices | Fast exponentiation by squaring (LeetCode 50 — Pow(x, n)) |
| Feature #8: Divide in Power Save Mode | Division via bit-shift doubling (LeetCode 29 — Divide Two Integers) |
| Feature #9: Validate Program Brackets | Stack-based bracket matching (LeetCode 20 — Valid Parentheses; LeetCode 678 — Valid Parenthesis String) |

The upcoming DIY problems let you practice each of these patterns directly, stripped of their compiler-specific framing.
