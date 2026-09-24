# DIY: Add Two Numbers

## Problem statement

Given two non-empty linked lists representing two non-negative integers, where the digits are stored in reverse order (the head is the least-significant digit) and each node holds a single digit, add the two numbers and return the sum as a linked list, in the same reversed digit order. You may assume neither number has a leading zero, except the number `0` itself.

### Input

```
3 -> 4 -> 6
2 -> 8 -> 1
```

### Output

```
5 -> 2 -> 8
```

Read least-significant-digit first, `3 -> 4 -> 6` is 643 and `2 -> 8 -> 1` is 182. Their sum, 825, written the same way (least-significant digit first), is `5 -> 2 -> 8`.

## Coding exercise

Implement `addTwoNumbers(list1, list2)`, returning the sum of the two numbers as a linked list.

This is the exact same pattern as [Feature #11: Weighted Exponential Back-off](11-feature-11-weighted-exponential-back-off.md) — there, we added two linked lists of back-off digits, least-significant digit first; here it's the bare pattern, no networking story. Walk both lists together with a running carry, appending one new digit per step to a result list built behind a dummy head.

## Solution

```java
class LinkedListNode {
    int val;
    LinkedListNode next;
    LinkedListNode(int val) { this.val = val; }
}

class Solution {
    public static LinkedListNode addTwoNumbers(LinkedListNode list1, LinkedListNode list2) {
        LinkedListNode dummy = new LinkedListNode(0);
        LinkedListNode current = dummy;
        LinkedListNode p = list1, q = list2;
        int carry = 0;

        while (p != null || q != null || carry != 0) {
            int x = (p != null) ? p.val : 0;
            int y = (q != null) ? q.val : 0;
            int sum = x + y + carry;
            carry = sum / 10;
            current.next = new LinkedListNode(sum % 10);
            current = current.next;
            if (p != null) p = p.next;
            if (q != null) q = q.next;
        }
        return dummy.next;
    }

    private static LinkedListNode build(int[] digits) {
        LinkedListNode dummy = new LinkedListNode(0);
        LinkedListNode current = dummy;
        for (int d : digits) {
            current.next = new LinkedListNode(d);
            current = current.next;
        }
        return dummy.next;
    }

    private static String toString(LinkedListNode node) {
        StringBuilder sb = new StringBuilder();
        while (node != null) {
            sb.append(node.val);
            if (node.next != null) sb.append(" -> ");
            node = node.next;
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        LinkedListNode list1 = build(new int[]{3, 4, 6});
        LinkedListNode list2 = build(new int[]{2, 8, 1});
        System.out.println(toString(addTwoNumbers(list1, list2)));
        // 5 -> 2 -> 8
    }
}
```

## Complexity measures

Let **m** and **n** be the lengths of `list1` and `list2`.

- **Time:** `O(max(m, n))` — the lists are walked in lockstep until both are exhausted and no carry remains.
- **Space:** `O(max(m, n))` — the result list has at most `max(m, n) + 1` nodes.
