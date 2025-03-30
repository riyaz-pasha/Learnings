# Threads

* A thread is lightweight sub-process, the smallest unit of execution within a process.
* Used for running task asynchronously.
* Javs provides buit in support for multithreading through `Thread` class and `Runnable` Interface.

**Why**

* *Concurrency and parallelism ( in multi core processors)* : Allows multiple tasks to run simultaniously.
* *Better CPU utilization*: Efficiently uses multi core processors.
* *Faster Execution*: Reduces execution time for parallel tasks.

### Thread Lifecycle

* **NEW**
  * Created but not yet started to run.
* **Runnable**
  * A thread that is ready to run moves to this thread. In this state thread can be running or might be ready to run. It is the responsibility of the thread schedular to give the thread time to run. A multi threaded program allocates fixed amount of time to run to each thread. After utilising it's time a thread pauses and gives up the CPU so that other threads can run.
* **Blocked**
  * Thread is in the blocled state when it's trying to acquire lock but the lock is acquired by the other thread already. This thread moves from blocked state to runnable state when acquired the lock.
* **Waiting**
  * The thread will be in the waiting state when it calls `wait()` or `join()` methods. It moves to the runnable state when other thread notifies or terminates.
* **Timed Waiting**
  *  The thread is waiting for a specific time duration.
* **Terminated**
  * Execution finished

### Volatile

* In a muti threaded environment, each thread keeps a cached copy of a variable instead of always reading from main memory. This can lead inconsitencies or stale data when 1 thread updates the variable and another thread reads the data.
* `volatile` ensures visibility and ordering.

* [](https://www.baeldung.com/java-volatile)

**How CPU Caching Works?**

* Modern CPUs has multi layered cache syatems (L1/L2/L2) to improve performance.
* This means,
  * Instead of always accessing main memory (RAM) (which is slow), CPUs store requrently used variables in CPU Registers and L1/L2/L3 caches.
  * Each CPU core has its own cache, leading to the visibility problem in multi threading.

**📌 Example of CPU Caching**

* Imagine a shared variable `flag = false`, which is stored in main memory initially.
* Thread 1 (CPU Core 1) reads flag → It loads it into its local cache (L1).
* Thread 2 (CPU Core 2) also reads flag → It loads a separate cached copy.
* Thread 2 updates `flag = true` → But the update is only in Core 2's cache, not in main memory!
* Thread 1 keeps checking flag → It never sees the update because it's still reading its cached copy!
* This is called the `cache coherence problem`. 

* [Example](./concepts/threads/VolatileExample.java)

### Synchronized

* When multiple thread access a shared resource simulaniously data inconsitency can occure due to race conditions.
* To prevent this race condition issues java provides synchronized keyword which allows only one thread to access the resource at a time.


### ReentrantLock

* The ReentrantLock class (from java.util.concurrent.locks) is an alternative to the synchronized keyword, providing more advanced locking mechanism

Unlike synchronized, ReentrantLock provides:
* ✅ Explicit lock/unlock control
* ✅ Ability to try locking without waiting
* ✅ Fairness policy
  

* Atomic variables in Java, like AtomicInteger, primarily operate within CPU caches (L1, L2) and use Compare-and-Swap (CAS) for atomic updates instead of directly writing to main memory. 

* Cache Coherence Protocols (MESI, MOESI)
  * Modern CPUs use cache coherence protocols like MESI (Modified, Exclusive, Shared, Invalid) to ensure all cores see the latest value.
  * When an atomic operation is performed:
    * It locks the cache line (L1 or L2) for modification.
    * Other cores must invalidate their cached copies.
    * This ensures all CPU cores see the latest value.  


### **How CAS (Compare-And-Swap) Works in Depth**  

Let's go step by step to understand what happens when two threads **simultaneously update an atomic variable (`AtomicInteger`) using CAS**.  

---

## **1. Initial State:**
Assume we have an `AtomicInteger counter = 5`.  

Now, **two threads (Thread 1 and Thread 2) running on different CPU cores try to increment `counter` at the same time**.

---

## **2. Thread 1 and Thread 2 Read `counter` Simultaneously**
- **Thread 1 (on CPU Core 1)** reads `counter = 5` into its L1 cache.  
- **Thread 2 (on CPU Core 2)** also reads `counter = 5` into its L1 cache.  

At this point, both threads think that `counter = 5` is the latest value.  

| CPU Core | Thread | Value Read |
|----------|--------|------------|
| Core 1 (L1 Cache) | Thread 1 | 5 |
| Core 2 (L1 Cache) | Thread 2 | 5 |

Both threads **now want to increment the value**.

---

## **3. Thread 2 Successfully Updates the Counter (CAS Success ✅)**
- **Thread 2 calculates the new value:** `5 + 1 = 6`.  
- **Thread 2 calls `compareAndSet(5, 6)`**:
  - It checks if `counter` in **main memory is still `5`**.
  - Since it **matches the expected value (`5`)**, the update **succeeds**, and `counter` becomes `6`.
- **Thread 2 writes `counter = 6` in its cache** and **invalidates all other copies** in other cores (MESI protocol).  

✅ **CAS succeeds for Thread 2**.  

| CPU Core | Thread | Value Read | CAS Operation |
|----------|--------|------------|--------------|
| Core 1 (L1 Cache) | Thread 1 | 5 | ❌ Still holds old value |
| Core 2 (L1 Cache) | Thread 2 | 5 → 6 | ✅ Updated successfully |

---

## **4. Thread 1 Tries to Update Counter (CAS Fails ❌)**
- **Thread 1 is still holding an outdated value (`counter = 5`) in its cache.**  
- It calculates: `5 + 1 = 6`.  
- **Thread 1 calls `compareAndSet(5, 6)`, but the expected value (`5`) is now `6` in memory** (due to Thread 2's update).  
- Since **expected value does not match the actual value**, **CAS fails** for Thread 1.  

❌ **Thread 1's update is rejected because its expected value (`5`) is stale.**  

| CPU Core | Thread | Value Read | CAS Operation |
|----------|--------|------------|--------------|
| Core 1 (L1 Cache) | Thread 1 | 5 | ❌ CAS fails (expected 5 but found 6) |
| Core 2 (L1 Cache) | Thread 2 | 5 → 6 | ✅ CAS succeeds |

---

## **5. Thread 1 Retries Until It Succeeds**
Since CAS failed, **Thread 1 does not give up**. Instead, it **retries the entire operation**:
1. **Thread 1 reloads `counter = 6` from memory.**
2. **Recalculates:** `6 + 1 = 7`.
3. **Attempts CAS again (`compareAndSet(6, 7)`).**
4. **Since `counter` is still `6`, CAS succeeds!** 🎉  

✅ **Thread 1 successfully updates the counter to `7` after retrying.**  

Final value in memory:  
```
counter = 7
```

| CPU Core | Thread | Value Read | CAS Operation |
|----------|--------|------------|--------------|
| Core 1 (L1 Cache) | Thread 1 | 6 → 7 | ✅ CAS succeeds |
| Core 2 (L1 Cache) | Thread 2 | 5 → 6 | ✅ CAS succeeded earlier |

---

## **6. Summary: What Just Happened?**
1. **Both threads read `counter = 5`.**
2. **Thread 2 updates `counter = 6` successfully** (CAS success ✅).
3. **Thread 1's CAS fails because `counter = 6` now**.
4. **Thread 1 retries, reads `6`, increments to `7`, and succeeds**.

### **Key Takeaways**
✅ **CAS ensures atomicity without using locks**.  
✅ **If CAS fails, the thread retries until it succeeds**.  
✅ **CPU cache coherence (MESI protocol) ensures all cores see the latest value**.  
✅ **This mechanism prevents race conditions and keeps updates thread-safe**.  

---

Would you like an **implementation example** of CAS behavior in Java? 🚀