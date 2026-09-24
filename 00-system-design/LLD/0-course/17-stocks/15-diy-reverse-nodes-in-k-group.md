# DIY: Reverse Nodes in k-Group

## Problem statement

Given a linked list, reverse the nodes of the linked list `k` at a time and return the modified list.

`k` is a positive integer and is less than or equal to the length of the linked list. If the number of nodes is not a multiple of `k`, the nodes left over at the end should remain in their original order.

You may not alter the values in the nodes of the list — only the nodes themselves may be rearranged.

> **Note:** Use only `O(1)` extra memory space.

### Input

```java
head = [1, 2, 3, 4, 5], k = 2
```

### Output

```java
[2, 1, 4, 3, 5]
```

## Coding exercise

Implement `reverseLinkedList(head, k)`.

The closest match in this chapter is [Feature #6: Assign Transactions](06-feature-6-assign-transactions.md) — it's the exact same problem, just told from the perspective of splitting transactions among brokers rather than as a bare linked-list exercise. The same `O(1)`-space, group-by-group reversal applies unchanged.

## Solution

```java
class Solution {
    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }

    // Reverses `head` k nodes at a time, leaving any trailing group of
    // fewer than k nodes untouched, using O(1) extra space.
    public static ListNode reverseLinkedList(ListNode head, int k) {
        ListNode newHead = null;
        ListNode ttail = null;
        ListNode remaining = head;

        while (hasKNodes(remaining, k)) {
            ListNode groupTail = remaining;
            ListNode afterGroup = advance(remaining, k);
            ListNode revHead = reverse(remaining, k);

            if (newHead == null) {
                newHead = revHead;
            }
            if (ttail != null) {
                ttail.next = revHead;
            }
            ttail = groupTail;
            remaining = afterGroup;
        }

        if (ttail != null) {
            ttail.next = remaining;
        }
        return newHead == null ? head : newHead;
    }

    private static boolean hasKNodes(ListNode head, int k) {
        int count = 0;
        while (head != null && count < k) {
            head = head.next;
            count++;
        }
        return count == k;
    }

    private static ListNode advance(ListNode node, int k) {
        for (int i = 0; i < k; i++) {
            node = node.next;
        }
        return node;
    }

    private static ListNode reverse(ListNode head, int k) {
        ListNode revHead = null;
        ListNode curr = head;
        for (int i = 0; i < k; i++) {
            ListNode next = curr.next;
            curr.next = revHead;
            revHead = curr;
            curr = next;
        }
        head.next = curr;
        return revHead;
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
        System.out.println(toStr(reverseLinkedList(head, 2)));
        // [2, 1, 4, 3, 5]
    }
}
```

Walking through `[1,2,3,4,5]` with `k = 2`: the first group `[1,2]` reverses to `[2,1]` (that becomes `newHead`, and `1` — the old head, now the tail — is remembered as `ttail`). The second group `[3,4]` reverses to `[4,3]`; `ttail` (`1`) gets wired to point at this group's new head (`4`), and `ttail` updates to `3`. Only one node (`5`) is left, which is fewer than `k = 2`, so `hasKNodes` returns false and the loop stops; `ttail` (`3`) gets wired to the untouched remainder (`5`). Final list: `2 -> 1 -> 4 -> 3 -> 5`.

## Complexity measures

Let **n** be the number of nodes in the linked list.

### Time Complexity

`O(n)` — every node is visited a constant number of times across the counting, reversing, and linking steps.

### Space Complexity

`O(1)` — reversal happens purely through pointer rewiring with a fixed set of helper references, no recursion or auxiliary structure.
