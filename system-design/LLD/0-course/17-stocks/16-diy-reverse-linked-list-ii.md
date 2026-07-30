# DIY: Reverse Linked List II

## Problem statement

Given the head of a singly linked list and two integers `left` and `right` (1-indexed positions, `left <= right`), reverse the nodes of the list from position `left` to position `right`, and return the modified list.

### Input

```java
head = [1, 2, 3, 4, 5], left = 2, right = 4
```

### Output

```java
[1, 4, 3, 2, 5]
```

## Coding exercise

Implement `reverseBetween(head, left, right)`.

The closest match in this chapter is [Feature #6: Assign Transactions](06-feature-6-assign-transactions.md) (also mirrored in [DIY: Reverse Nodes in k-Group](15-diy-reverse-nodes-in-k-group.md)) — both problems are "reverse a contiguous run of a singly linked list in place, `O(1)` space." The k-group version reverses *every* run of a fixed size; this version reverses exactly *one* run, delimited by explicit `left`/`right` positions instead of a group size. The same head-insertion reversal technique — walk the segment, splicing each node onto the front of a small "reversed so far" list — does the job for both.

## Solution

```java
class Solution {
    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }

    // Reverses the nodes from 1-indexed position `left` to `right`
    // (inclusive) and returns the (possibly new) head of the list.
    public static ListNode reverseBetween(ListNode head, int left, int right) {
        ListNode dummy = new ListNode(0);
        dummy.next = head;

        ListNode leftPrev = dummy; // Node right before the segment to reverse.
        for (int i = 0; i < left - 1; i++) {
            leftPrev = leftPrev.next;
        }

        ListNode curr = leftPrev.next; // Will end up as the segment's tail.
        ListNode prev = null;
        for (int i = 0; i < right - left + 1; i++) {
            ListNode next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }

        // prev = new head of the reversed segment; curr = first node after it.
        leftPrev.next.next = curr; // Old segment head, now its tail, hooks back on.
        leftPrev.next = prev;

        return dummy.next;
    }

    static ListNode fromArray(int[] arr) {
        ListNode dummy = new ListNode(0), tail = dummy;
        for (int v : arr) {
            tail.next = new ListNode(v);
            tail = tail.next;
        }
        return dummy.next;
    }

    static String toStr(ListNode head) {
        StringBuilder sb = new StringBuilder("[");
        while (head != null) {
            sb.append(head.val);
            if (head.next != null) sb.append(", ");
            head = head.next;
        }
        return sb.append("]").toString();
    }

    public static void main(String[] args) {
        ListNode head = fromArray(new int[]{1, 2, 3, 4, 5});
        System.out.println(toStr(reverseBetween(head, 2, 4)));
        // [1, 4, 3, 2, 5]
    }
}
```

A dummy node in front of `head` sidesteps the special case where `left == 1` (i.e., the reversal starts at the very head of the list) — `leftPrev` can always walk forward from `dummy` without needing an `if` for "is there a node before the segment at all?" `leftPrev` stops at the node just before position `left`; `curr` then starts the reversal loop and, by the time it's done `right - left + 1` iterations, `prev` is the new head of the reversed segment and `curr` is the node right after it. Since `leftPrev.next` still points at the *old* head of the segment (which is now its tail after reversal), `leftPrev.next.next = curr` reattaches the tail to what follows, and only then do we overwrite `leftPrev.next` itself to point at the new segment head, `prev`.

Tracing `[1,2,3,4,5]` with `left=2, right=4`: `leftPrev` stops at node `1`. The loop reverses `2 -> 3 -> 4` into `4 -> 3 -> 2`, with `prev = 4` and `curr = 5` (the node after the segment) once it's done. `leftPrev.next` (still `2`) gets `.next` set to `5` (attaching the segment's tail to what follows), then `leftPrev.next` is set to `4` (the new segment head). Final list: `1 -> 4 -> 3 -> 2 -> 5`.

## Complexity measures

Let **n** be the number of nodes in the linked list.

### Time Complexity

`O(n)` — in the worst case (`left = 1, right = n`), every node is visited once while walking to `left` and once while reversing.

### Space Complexity

`O(1)` — reversal uses a fixed number of pointer variables; no recursion or extra list is allocated.
