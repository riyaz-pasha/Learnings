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
  