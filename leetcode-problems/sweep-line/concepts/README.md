# 🧹 Sweep Line Algorithm – A Simple Guide

## 🧠 What is it?

The **Sweep Line Algorithm** is a method where we "sweep" a vertical line from **left to right** across a set of events (like intervals or points), and process things as we go.

Think of it like:  
> 👨‍🏫 "Hey, at each point in time or space, what’s currently happening?"

---

## 💡 Key Idea

1. ✂️ Break input into **events** (like start or end of intervals).
2. 🔢 Sort the events (by position or time).
3. 🚶 Sweep from left to right, **keeping track** of what's active.

---

## ✅ When to Use Sweep Line

Use when the problem involves:
- 📅 Intervals (start & end times)
- 🔁 Overlaps or conflicts
- 🧮 Counting what's "active" at a time
- 📈 Tracking changes over time/space

---

## 🧩 Real-life Examples
- 📆 Count how many meetings happen at the same time.
- 🏢 See what the skyline of a city looks like.
- 🚌 Check if you can fit passengers in a bus over time.

---

## 🪜 Step-by-Step Process

1. **Create Events**
   - For each interval `(start, end)`, make two events:
     - `{time=start, type=+1}` (starting point)
     - `{time=end, type=-1}` (ending point)

2. **Sort Events**
   - Sort by time.
   - If time is same, sort by type (end before start).

3. **Sweep**
   - Walk through the events.
   - Use a counter to track active intervals.

---

## 🔍 Example: Max Overlapping Intervals

### 📥 Input:
```java
[(1, 5), (2, 6), (4, 7), (5, 8)]
