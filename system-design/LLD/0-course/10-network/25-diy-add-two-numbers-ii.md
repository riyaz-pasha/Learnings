# DIY: Add Two Numbers II

## Problem statement

Given two non-empty linked lists representing two non-negative integers, where the digits are stored with the most-significant digit first (the head is the most-significant digit) and each node holds a single digit, add the two numbers and return the sum as a linked list, in the same most-significant-digit-first order. You may assume neither number has a leading zero, except the number `0` itself. You may not modify the input lists' order (no reversing them in place).

### Input

```
// Example 1
3 -> 4 -> 6
2 -> 8 -> 1

// Example 2
1 -> 7 -> 9 -> 2
     2 -> 8 -> 1
```

### Output

```
// Example 1
6 -> 2 -> 7

// Example 2
2 -> 0 -> 7 -> 3
```

`346 + 281 = 627`, and `1792 + 281 = 2073`.

## Coding exercise

Implement `addTwoNumbers(list1, list2)`, returning the sum of the two numbers as a linked list, most-significant digit first.

This is the same digit-by-digit addition as [Feature #11: Weighted Exponential Back-off](11-feature-11-weighted-exponential-back-off.md), but with the digit order flipped end for end. Since addition has to start from the least-significant digit, and here that's at the *tail* of each list, we can't just walk both lists head to tail like before — we first need to see each list from the back. Pushing each list's digits onto a stack does exactly that: the stack's pop order is tail-to-head, i.e., least-significant digit first. Pop the top of each stack in lockstep, add with carry, and prepend (not append) each new digit to the result, since the digits come out in least-to-most-significant order but the result must read most-significant first.

## Solution

```java
import java.util.*;

class LinkedListNode {
    int val;
    LinkedListNode next;
    LinkedListNode(int val) { this.val = val; }
}

class Solution {
    public static LinkedListNode addTwoNumbers(LinkedListNode list1, LinkedListNode list2) {
        Deque<Integer> stack1 = new ArrayDeque<>();
        Deque<Integer> stack2 = new ArrayDeque<>();
        while (list1 != null) { stack1.push(list1.val); list1 = list1.next; }
        while (list2 != null) { stack2.push(list2.val); list2 = list2.next; }

        int carry = 0;
        LinkedListNode result = null;

        while (!stack1.isEmpty() || !stack2.isEmpty() || carry != 0) {
            int x = stack1.isEmpty() ? 0 : stack1.pop();
            int y = stack2.isEmpty() ? 0 : stack2.pop();
            int sum = x + y + carry;
            carry = sum / 10;

            LinkedListNode node = new LinkedListNode(sum % 10);
            node.next = result; // prepend, since digits arrive least-significant first
            result = node;
        }
        return result;
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
        System.out.println(toString(addTwoNumbers(build(new int[]{3, 4, 6}), build(new int[]{2, 8, 1}))));
        // 6 -> 2 -> 7

        System.out.println(toString(addTwoNumbers(build(new int[]{1, 7, 9, 2}), build(new int[]{2, 8, 1}))));
        // 2 -> 0 -> 7 -> 3
    }
}
```

## Complexity measures

Let **m** and **n** be the lengths of `list1` and `list2`.

- **Time:** `O(max(m, n))` — pushing both lists onto stacks takes `O(m + n)`, and popping them in lockstep takes `O(max(m, n))`.
- **Space:** `O(max(m, n))` — for the two stacks and the result list.
