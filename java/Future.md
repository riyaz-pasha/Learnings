# Future

* Future<T> is an interface of java's concurrency framework and represents the result of an async computation.
* It allows us to execute task async and retrieve result once the computation is complete.
* Simply put, the Future class represents a future result of an asynchronous computation. This result will eventually appear in the Future after the processing is complete.

**Limitations**

1. No callbacks on completion: must use get() for result which is a blocking call
2. No way to manually complete a future: Once task is submitted, we cannot explicitly complete it.
3. No chaining of futures.

# Completable Future