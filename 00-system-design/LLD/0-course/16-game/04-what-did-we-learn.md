# What Did We Learn?

## What have we accomplished?

The problems we solved to implement our Game features are also asked in some of the most popular interview questions at top-tier companies. You can now identify problems with the same underlying pattern and solve them using the techniques you learned here.

Here's the map from what we built to the pattern it was really testing:

| Game Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Hand of Straights | Greedy grouping from the smallest remaining element, using a sorted frequency map to check and consume whole runs of consecutive values | Hand of Straights |
| #2 Maximum Points You Can Obtain from Cards | Fixed-size sliding window that starts pinned to one end of the array and slides across, tracking the running sum incrementally | Maximum Points You Can Obtain from Cards |
| #3 Balloon Splash | Stack of (character, run-length) pairs that pops whenever a run reaches a target length, letting newly-adjacent runs chain-react automatically | Remove All Adjacent Duplicates in String II |

The three features look unrelated on the surface — poker hands, a card-picking game, popping balloons — but each is really a well-known array/string pattern wearing a costume. Feature #1 is a greedy-with-a-frequency-map problem: once you sort the values, the only sane way to build a group is to start from the smallest leftover card, because nothing smaller is around to pair with it — if that specific run isn't fully present, no rearrangement can save the hand. Feature #2 is a sliding-window problem in disguise: "pick some prefix from the left and the rest from the right" is equivalent to sliding a fixed-size window across the array from one end to the other, since whichever cards you *don't* pick always form one contiguous block in the middle. Feature #3 is a stack problem: the key insight is that removing a run can expose a brand-new run right at the seam, and a stack of (character, count) pairs handles that chain reaction naturally, without ever re-scanning the string.

The DIY problems that follow are the same three patterns again, stripped of the card-game and balloon-game framing — solve them to prove the pattern really transferred, not just the specific solution. Notice that two of the DIYs (Remove All Adjacent Duplicates In String, and its "II" sequel) both map back to Balloon Splash: the first is the special case `k = 2`, and the second is the exact same general-`k` stack algorithm Balloon Splash already uses.
