# 📈 Kadane’s Algorithm – Simplified Explanation

## 🧩 Problem Statement

Given an array of integers (both positive and negative), find the **maximum sum of a contiguous subarray**.

> **Example:**
> Input: `[4, -1, 2, 1, -5, 4]`
> Output: `6`
> Explanation: The subarray `[4, -1, 2, 1]` has the maximum sum.

---

## 🧠 Real-Life Analogy

Imagine walking on a road where each step gives (positive number) or takes away (negative number) money.
You can **start walking at any point and stop at any point**, but your goal is to **maximize the money in your pocket**.

* If your total money goes negative, it's smarter to **start over** from the next step.

---

## 🧾 Kadane’s Core Logic

Maintain two variables:

* `currentSum` → Current subarray sum (money in pocket)
* `maxSum` → Maximum subarray sum found so far (most money you've had)

### ✅ Algorithm:

```python
def kadane(arr):
    maxSum = float('-inf')  # Start with smallest possible number
    currentSum = 0

    for num in arr:
        currentSum += num              # Step forward
        maxSum = max(maxSum, currentSum)  # Best so far?
        if currentSum < 0:
            currentSum = 0             # Restart from next

    return maxSum
```
```java
public class KadaneAlgorithm {

    public static int maxSubArray(int[] nums) {
        int maxSum = Integer.MIN_VALUE;
        int currentSum = 0;

        for (int num : nums) {
            currentSum += num;

            if (currentSum > maxSum) {
                maxSum = currentSum;
            }

            if (currentSum < 0) {
                currentSum = 0; // reset if negative
            }
        }

        return maxSum;
    }

    public static void main(String[] args) {
        int[] arr = {4, -1, 2, 1, -5, 4};
        int result = maxSubArray(arr);
        System.out.println("Maximum Subarray Sum: " + result); // Output: 6
    }
}
```

---

## 🔁 Step-by-Step Example

**Input:** `[4, -1, 2, 1, -5, 4]`

| Step | Num | currentSum | maxSum |
| ---- | --- | ---------- | ------ |
| 1    | 4   | 4          | 4      |
| 2    | -1  | 3          | 4      |
| 3    | 2   | 5          | 5      |
| 4    | 1   | 6          | 6 ✅    |
| 5    | -5  | 1          | 6      |
| 6    | 4   | 5          | 6      |

> ✅ Final Answer: `6`

---

## 🧠 Easy Way to Remember

> **"Add, Compare, Reset if Negative"**

1. **Add** current number to running sum
2. **Compare** and update `maxSum` if needed
3. **Reset** `currentSum` to `0` if it drops below `0`

---

## ⏱️ Time and Space Complexity

* **Time Complexity:** `O(n)`
* **Space Complexity:** `O(1)`


---

## 🧩 Problem: Max Subarray Sum in Circular Array

You're given an array, but it's **circular**.

That means:

* After the last element, you can wrap around and continue from the first element.
* You want to pick a **contiguous chunk** (subarray) with the **maximum sum**.

---

## 🧠 Two Main Scenarios

### ✅ **Case 1: Normal subarray (no wrap)**

This is the usual case like in Kadane’s Algorithm.

**Example:**
`[1, -2, 3, -2]`
→ Pick `[3]` → Sum = `3`

---

### 🔁 **Case 2: Circular subarray (wrap-around)**

Now imagine the array loops.

**Example:**
`[5, -3, 5]`
If you wrap: `[5, ..., 5]` → sum = `5 + 5 = 10`
Ignore the middle `-3` → Total = sum of array − min subarray

---

## 🧠 Super Simple Trick:

Use this formula:

```
Max circular sum = max(normal sum, total sum - min subarray sum)
```

Let’s now **spoon-feed** the whole approach with examples 👇

---

## 🪜 Step-by-Step Plan (with examples)


Example input:

    [1, -2, 3, -2]

---

## 🔑 Key Insight

The maximum circular subarray sum is one of two cases:

1. **Non-circular subarray** → Standard Kadane’s Algorithm
2. **Circular subarray** → Total array sum minus minimum subarray sum

Final formula:

    max(normalMax, circularMax)

---

## 🔢 Step 1: Find the Maximum Subarray Sum (Kadane’s Algorithm)

Array:

    [1, -2, 3, -2]

Using Kadane’s algorithm:
- The maximum subarray sum is `3`
- Achieved by the subarray `[3]`

Result:

    maxKadane = 3

---

## ➕ Step 2: Compute the Total Sum of the Array

Calculation:

    1 + (-2) + 3 + (-2) = 0

Result:

    totalSum = 0

---

## 🔻 Step 3: Find the Minimum Subarray Sum

To handle the circular case, we compute the **minimum subarray sum**
using a modified Kadane’s algorithm.

- The minimum subarray sum is `-2`
- Achieved by the subarray `[-2]`
- (There are two such positions; only the value matters)

Result:

    minKadane = -2

---

## 🔁 Step 4: Compute the Circular Subarray Sum

A circular maximum subarray can be interpreted as:
taking the total sum of the array and excluding the minimum subarray.

Calculation:

    circularSum = totalSum - minKadane
                 = 0 - (-2)
                 = 2

---

## ✅ Step 5: Compute the Final Result

The maximum circular subarray sum is the maximum of:
- the non-circular maximum subarray sum
- the circular subarray sum

Calculation:

    max(maxKadane, circularSum)
    = max(3, 2)
    = 3

Final Answer:

    3

---

## 🧠 Intuition Summary

- **Normal max subarray:** What we want to keep
- **Minimum subarray:** What we want to remove
- **Circular max:** Total sum minus the worst contiguous part
- Non-circular subarray can still be optimal

---

## ⏱️ Time and Space Complexity

Time Complexity:

    O(n)

Space Complexity:

    O(1)

---

---

## 🎯 Now Try: `[5, -3, 5]`

1. Normal Kadane: max sum = `7` (`[5, -3, 5]`)
2. Total sum = `5 + (-3) + 5 = 7`
3. Min subarray = `[-3]`
4. Circular sum = `7 - (-3) = 10`
5. Final max = `max(7, 10)` = `10` ✅

✔ Answer: `10`

---

## ⚠️ Edge Case: All Negative Numbers

**Example:** `[-3, -2, -1]`

1. Normal max = `-1`
2. Total = `-6`
3. Min subarray = `-6`
4. Circular sum = `-6 - (-6) = 0 ❌ (invalid)`

So if **all numbers are negative**, just return the **maximum single number** (Kadane result).
✅ Answer: `-1`

---

## ✅ Final Java Code with Comments

```java
public class CircularMaxSum {

    public static int maxSubarraySumCircular(int[] nums) {
        int total = 0;
        int maxSum = Integer.MIN_VALUE, curMax = 0;
        int minSum = Integer.MAX_VALUE, curMin = 0;

        for (int num : nums) {
            // 1. For normal max subarray (Kadane)
            curMax = Math.max(curMax + num, num);
            maxSum = Math.max(maxSum, curMax);

            // 2. For min subarray (to use in circular case)
            curMin = Math.min(curMin + num, num);
            minSum = Math.min(minSum, curMin);

            // 3. Total array sum
            total += num;
        }

        // 4. If all are negative, return normal Kadane result
        if (maxSum < 0) {
            return maxSum;
        }

        // 5. Else, return max(normal, circular)
        return Math.max(maxSum, total - minSum);
    }

    public static void main(String[] args) {
        int[] nums1 = {1, -2, 3, -2};      // ➞ 3
        int[] nums2 = {5, -3, 5};          // ➞ 10
        int[] nums3 = {-3, -2, -1};        // ➞ -1

        System.out.println(maxSubarraySumCircular(nums1));
        System.out.println(maxSubarraySumCircular(nums2));
        System.out.println(maxSubarraySumCircular(nums3));
    }
}
```

---

## 🔁 Recap

| Case Type    | Logic                             |
| ------------ | --------------------------------- |
| Normal       | Use Kadane’s Algorithm            |
| Circular     | `total sum - min subarray sum`    |
| All Negative | Return Kadane result (don’t wrap) |

---
