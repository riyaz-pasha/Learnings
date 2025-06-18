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

### 🔢 Step 1: Find normal max subarray (Kadane)

**Array:** `[1, -2, 3, -2]`
Kadane gives `3` (`[3]`)

---

### ➕ Step 2: Find total sum

`1 + (-2) + 3 + (-2) = 0`

---

### 🔻 Step 3: Find min subarray sum

Min subarray = `[-2, 3, -2]` → sum = `-1`

---

### 🔁 Step 4: Calculate circular sum

`circular sum = total sum - min subarray = 0 - (-1) = 1`

---

### ✅ Step 5: Final result = max(normal sum, circular sum)

`max(3, 1) = 3`
✔ Answer: `3`

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