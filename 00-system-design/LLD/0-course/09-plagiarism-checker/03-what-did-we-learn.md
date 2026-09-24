# What Did We Learn?

## What have we accomplished?

The problems we solved to build our Plagiarism Checker features are also asked, almost word for word, in some of the most popular coding interview questions at top-tier companies. Once you recognize the pattern, you can spot it under any story.

Here's the map from what we built to the pattern it was really testing:

| Plagiarism Checker Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Possible Matches | Bucket-and-advance subsequence matching, one pass over the source string | Number of Matching Subsequences |
| #2 Return Match | Forward subsequence scan + backward window shrink | Minimum Window Subsequence |

Both features boil down to the same core idea: a **subsequence match with noise tolerance**. A cheater's disguise — extra tokens wedged between the copied content — is exactly the kind of "junk in between" that a subsequence check (rather than a substring check) is built to see through. Feature #1 asks "does a match exist, across many candidates, in one pass?" and Feature #2 asks "given a match exists, what's the tightest evidence of it?" Those two questions cover a surprising number of "find the hidden match" interview problems.

The DIY problems that follow are the same two patterns again, stripped of the Plagiarism Checker story — solve them to prove the pattern really transferred, not just the specific solution.
