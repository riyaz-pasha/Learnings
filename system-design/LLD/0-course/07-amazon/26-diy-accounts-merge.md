# DIY: Accounts Merge

## Problem statement

You are given a 2D array `accounts`. Each `accounts[i]` is a list of strings where `accounts[i][0]` is a name and the rest are emails belonging to that account. Merge accounts that belong to the same person: two accounts belong to the same person if they share at least one email. Note that two people can share the same name — accounts only merge if they share an email — but every account belonging to one person will list the same name.

Return the merged accounts, where each account is the name followed by all of that person's distinct emails in sorted order.

### Input

```java
accounts = [
    ["Micheal", "michealjordan@mail.com", "mikey@mail.com"],
    ["Lily", "Lily4@mail.com", "Lily6@mail.com", "Lily7@mail.com"],
    ["Lily", "lilysmith@mail.com", "lily@mail.com"],
    ["Micheal", "mikey@mail.com", "micheal2@mail.com"]
]
```

### Output

```java
[
    ["Micheal", "michealjordan@mail.com", "micheal2@mail.com", "mikey@mail.com"],
    ["Lily", "Lily4@mail.com", "Lily6@mail.com", "Lily7@mail.com"],
    ["Lily", "lily@mail.com", "lilysmith@mail.com"]
]
```

(The two `"Micheal"` accounts merge because they share `mikey@mail.com`. The two `"Lily"` accounts stay separate — same name, but no shared email — so they belong to two different people. The order of the merged accounts in the output, and which account "wins" as the representative, doesn't matter, only the grouping and each group's sorted emails do.)

## Coding exercise

Implement `accountsMerge(accounts)`.

This is the exact same pattern as [Feature #8: Merge Recommendations](08-feature-8-merge-recommendations.md) — there, Amazon needed to merge product recommendation records coming in from an acquired company; here it's the bare pattern with no story attached. Treat every email as a node in a union-find structure: for each account, union all of its emails together. Once every account has been processed, group emails by their root parent, attach the account's name to each group, and sort the emails.

## Solution

```java
import java.util.*;

class Solution {
    public static List<List<String>> accountsMerge(String[][] accounts) {
        Map<String, String> parent = new HashMap<>();
        Map<String, String> emailToName = new HashMap<>();

        // Union every email in an account together, using the account's first
        // email as the pivot.
        for (String[] account : accounts) {
            String name = account[0];
            for (int i = 1; i < account.length; i++) {
                String email = account[i];
                parent.putIfAbsent(email, email);
                emailToName.put(email, name);
                union(parent, account[1], email);
            }
        }

        // Group emails by their root parent — each group is one person.
        Map<String, TreeSet<String>> groups = new HashMap<>();
        for (String email : parent.keySet()) {
            String root = find(parent, email);
            groups.computeIfAbsent(root, k -> new TreeSet<>()).add(email);
        }

        List<List<String>> result = new ArrayList<>();
        for (Map.Entry<String, TreeSet<String>> entry : groups.entrySet()) {
            List<String> merged = new ArrayList<>();
            merged.add(emailToName.get(entry.getKey()));
            merged.addAll(entry.getValue()); // TreeSet keeps emails sorted
            result.add(merged);
        }
        return result;
    }

    private static String find(Map<String, String> parent, String x) {
        while (!parent.get(x).equals(x)) {
            parent.put(x, parent.get(parent.get(x))); // path compression
            x = parent.get(x);
        }
        return x;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        String rootA = find(parent, a);
        String rootB = find(parent, b);
        if (!rootA.equals(rootB)) {
            parent.put(rootA, rootB);
        }
    }

    public static void main(String[] args) {
        String[][] accounts = {
            {"Micheal", "michealjordan@mail.com", "mikey@mail.com"},
            {"Lily", "Lily4@mail.com", "Lily6@mail.com", "Lily7@mail.com"},
            {"Lily", "lilysmith@mail.com", "lily@mail.com"},
            {"Micheal", "mikey@mail.com", "micheal2@mail.com"}
        };
        for (List<String> merged : accountsMerge(accounts)) {
            System.out.println(merged);
        }
        // [Lily, lily@mail.com, lilysmith@mail.com]
        // [Lily, Lily4@mail.com, Lily6@mail.com, Lily7@mail.com]
        // [Micheal, micheal2@mail.com, michealjordan@mail.com, mikey@mail.com]
    }
}
```

## Complexity measures

Let **n** be the total number of accounts and **k** the total number of emails across all accounts.

- **Time:** `O(k log k)` — near-constant-time union-find operations for each email, dominated by sorting the emails within each group.
- **Space:** `O(k)` — the union-find parent map and the name map each hold one entry per distinct email.
