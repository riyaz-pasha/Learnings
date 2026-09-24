# DIY: Find Duplicate Files in System

## Problem statement

You're given `paths`, a list of directory descriptions. Each entry has the format:

```
"root/dir1/dir2/.../dirm f1.txt(f1_content) f2.txt(f2_content) ... fn.txt(fn_content)"
```

That means: in directory `root/dir1/.../dirm`, there are `n` files, each with a name and its content in parentheses. Return a list of groups, where each group contains the full paths of every file (across the whole file system) that shares identical content with at least one other file. Groups of size 1 (files with unique content) are excluded. Order doesn't matter.

### Input

```java
// Sample 1
// paths = ["root/a 4.txt(xyz) 1.txt(algorithms)","root/c 3.txt(educative)","root/c/d 2.txt(algorithms)","root 4.txt(educative) 5.txt(abcd)"]

// Sample 2
// paths = ["root 1.txt(abcd) 2.txt(algo)","root/a 2.txt(abcd)","root/c/d 4.txt(algo)"]

// Sample 3
// paths = ["root 1.txt(abcd) 2.txt(algo)","root/a 2.txt(xyzc)","root/c/d 4.txt(educative)"]
```

### Output

```java
// Sample 1
// [["root/a/1.txt","root/c/d/2.txt"],["root/c/3.txt","root/4.txt"]]

// Sample 2
// [["root/2.txt","root/c/d/4.txt"],["root/1.txt","root/a/2.txt"]]

// Sample 3
// []
```

## Coding exercise

Implement `findDuplicate(paths)`.

This is exactly [Feature #7: Trending Hashtags](07-feature-7-trending-hashtags.md) — the same "bucket by a derived key, report buckets with two or more entries" pattern. There, the key was a hashtag and the bucketed value was a `day/person` path; here, the key is a file's content and the bucketed value is its full `directory/filename` path.

## Solution

```java
import java.util.*;

class Solution {
    // Groups file paths by their content, returning one group per content
    // value that's shared by two or more files.
    public static List<List<String>> findDuplicate(String[] paths) {
        Map<String, List<String>> map = new HashMap<>();

        for (String entry : paths) {
            String[] tokens = entry.split(" ");
            String dir = tokens[0];

            for (int i = 1; i < tokens.length; i++) {
                int open = tokens[i].indexOf('(');
                String fileName = tokens[i].substring(0, open);
                String content = tokens[i].substring(open + 1, tokens[i].length() - 1);
                String path = dir + "/" + fileName;

                map.computeIfAbsent(content, k -> new ArrayList<>()).add(path);
            }
        }

        List<List<String>> output = new ArrayList<>();
        for (List<String> groupedPaths : map.values()) {
            if (groupedPaths.size() >= 2) {
                output.add(groupedPaths);
            }
        }
        return output;
    }

    public static void main(String[] args) {
        String[] sample1 = {
            "root/a 4.txt(xyz) 1.txt(algorithms)",
            "root/c 3.txt(educative)",
            "root/c/d 2.txt(algorithms)",
            "root 4.txt(educative) 5.txt(abcd)"
        };
        System.out.println(findDuplicate(sample1));
        // [[root/a/1.txt, root/c/d/2.txt], [root/c/3.txt, root/4.txt]]

        String[] sample2 = {
            "root 1.txt(abcd) 2.txt(algo)",
            "root/a 2.txt(abcd)",
            "root/c/d 4.txt(algo)"
        };
        System.out.println(findDuplicate(sample2));
        // [[root/2.txt, root/c/d/4.txt], [root/1.txt, root/a/2.txt]]

        String[] sample3 = {
            "root 1.txt(abcd) 2.txt(algo)",
            "root/a 2.txt(xyzc)",
            "root/c/d 4.txt(educative)"
        };
        System.out.println(findDuplicate(sample3));
        // []
    }
}
```

## Solution walkthrough

Each directory entry is split by spaces: the first token is the directory path, and every following token has the shape `filename(content)`. For each file, we build its full path (`directory + "/" + filename`) and drop it into a map keyed by that file's content. In sample 1, `algorithms` content appears in both `root/a/1.txt` and `root/c/d/2.txt`, and `educative` content appears in both `root/c/3.txt` and `root/4.txt` — both groups have 2+ members, so both are reported; `xyz` and `abcd` appear only once each, so they're dropped. Sample 3 has no repeated content at all, so the output is empty.

## Complexity measures

Let **n** be the number of directory entries in `paths` and **x** be the average length of an entry string.

### Time Complexity

`O(n * x)` — every entry is parsed character by character.

### Space Complexity

`O(n * x)` — in the worst case (no duplicate content), the map ends up holding every parsed path.
