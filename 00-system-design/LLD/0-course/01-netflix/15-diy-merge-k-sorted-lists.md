# DIY: Merge K Sorted Lists

## Problem statement

Given multiple sorted linked lists, merge them into one sorted linked list.

### Input

```java
{
  {2, 4, 6, 8, 10},
  {1, 3, 5, 7, 9}
}
```

### Output

```java
{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}
```

## Coding exercise

Implement `mergeKLists(lists)`, where `lists` is a list of sorted linked lists, returning one fully merged sorted list.

This is the bare version of [Feature #2: Fetch Top Movies](02-feature-2-fetch-top-movies.md), where the "lists" were per-country movie rankings. Fold the lists together two at a time, reusing a standard two-list merge.

## Solution

```java
import java.util.List;

class LinkedListNode {
    int data;
    LinkedListNode next;

    LinkedListNode(int data) {
        this.data = data;
    }
}

class Solution {

    public static LinkedListNode mergeKLists(List<LinkedListNode> lists) {
        if (lists == null || lists.isEmpty()) {
            return null;
        }

        LinkedListNode result = lists.get(0);
        for (int i = 1; i < lists.size(); i++) {
            result = merge2Lists(result, lists.get(i));
        }
        return result;
    }

    private static LinkedListNode merge2Lists(LinkedListNode l1, LinkedListNode l2) {
        LinkedListNode dummy = new LinkedListNode(-1);
        LinkedListNode prev = dummy;

        while (l1 != null && l2 != null) {
            if (l1.data <= l2.data) {
                prev.next = l1;
                l1 = l1.next;
            } else {
                prev.next = l2;
                l2 = l2.next;
            }
            prev = prev.next;
        }
        prev.next = (l1 != null) ? l1 : l2;

        return dummy.next;
    }

    private static LinkedListNode fromArray(int[] values) {
        LinkedListNode dummy = new LinkedListNode(-1);
        LinkedListNode curr = dummy;
        for (int v : values) {
            curr.next = new LinkedListNode(v);
            curr = curr.next;
        }
        return dummy.next;
    }

    public static void main(String[] args) {
        List<LinkedListNode> lists = List.of(
                fromArray(new int[]{2, 4, 6, 8, 10}),
                fromArray(new int[]{1, 3, 5, 7, 9})
        );

        LinkedListNode merged = mergeKLists(lists);
        StringBuilder sb = new StringBuilder();
        for (LinkedListNode n = merged; n != null; n = n.next) {
            sb.append(n.data).append(" ");
        }
        System.out.println(sb.toString().trim()); // 1 2 3 4 5 6 7 8 9 10
    }
}
```

## Complexity measures

Let **k** be the number of lists and **n** the maximum length of a single list.

- **Time:** `O(n × k²)` — the running merged result can grow to `~(k-1) × n` elements, and it gets re-merged with a new `n`-element list `k - 1` times.
- **Space:** `O(1)` extra — nodes are relinked in place, no new list storage.
