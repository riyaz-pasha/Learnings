# DIY: First Bad Version

## Problem statement

You manage a project with `n` versions, numbered `1` through `n`. You have access to an API, `isBadVersion(version)`, which returns `true` if a version is bad. Since each version is built on top of the previous one, once a version is bad, every version after it is also bad. Find the first bad version, using as few API calls as possible.

### Input

```java
int n = 23;
```

(with the API reporting versions `19` through `23` as bad)

### Output

```java
19
```

## Coding exercise

Implement `firstBadVersion(n)`.

This is the exact same pattern as [Feature #5: Compilation Step Failure](05-feature-5-compilation-step-failure.md) — there, the compiler needed to find the first build step that failed among a run of successful-then-failing steps; here it's the bare "first true in a sorted boolean sequence" pattern with no story attached. The binary search transfers over unchanged.

## Solution

```java
class Solution {
    interface VersionControl {
        boolean isBadVersion(int version);
    }

    public static int firstBadVersion(int n, VersionControl api) {
        int first = 1;
        int last = n;
        while (first < last) {
            int mid = first + (last - first) / 2;
            if (api.isBadVersion(mid)) {
                last = mid;
            } else {
                first = mid + 1;
            }
        }
        return first;
    }

    public static void main(String[] args) {
        VersionControl demo = version -> version >= 19; // versions 19..23 are bad.
        System.out.println(firstBadVersion(23, demo));
        // 19
    }
}
```

Each API call halves the search space: if `mid` is bad, the first bad version is `mid` or earlier; otherwise, it's strictly later. The loop ends when `first == last`, which is the answer.

## Complexity measures

Let **n** be the total number of versions.

- **Time:** `O(log n)` — binary search over the version range.
- **Space:** `O(1)` — a fixed number of integer pointers.
