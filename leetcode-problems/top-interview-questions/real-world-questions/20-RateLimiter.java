import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

class User {
}

class FixedWindow {

    long windowStartTime;
    int count;

    FixedWindow(long startTime) {
        this.windowStartTime = startTime;
        this.count = 1;
    }

    public void incrementCount() {
        this.count++;
    }

    public void reset(long startTime) {
        this.windowStartTime = startTime;
        this.count = 1;
    }
}

class FixedWindowRateLimiter {

    private final int requestsPerMinute;
    private final ConcurrentHashMap<User, FixedWindow> userRequestMap = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public boolean isAllowed(User user) {
        long currentTime = System.currentTimeMillis();

        this.userRequestMap.putIfAbsent(user, new FixedWindow(currentTime));

        FixedWindow userWindow = this.userRequestMap.get(user);

        synchronized (userWindow) {
            long elapsedTime = currentTime - userWindow.windowStartTime;
            if (elapsedTime > TimeUnit.MINUTES.toMillis(1)) {
                // New window has started, reset the count
                userWindow.reset(currentTime);
                return true;
            } else {
                if (userWindow.count < this.requestsPerMinute) {
                    userWindow.incrementCount();
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

}

class SlidingWindowLogRateLimiter {

    private final int requestsPerMinute;
    private final ConcurrentHashMap<User, Deque<Long>> userRequestLogs = new ConcurrentHashMap<>();

    public SlidingWindowLogRateLimiter(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    /*
     * Time per request: O(1) amortized (deque trimming is quick)
     * Space: O(U × N), where U is the number of users and N is the max requests
     */
    public boolean isAllowed(User user) {
        long currentTime = System.currentTimeMillis();
        long oneMinuteAgo = currentTime - TimeUnit.MINUTES.toMillis(1);

        // Put an empty deque if the user is not present
        this.userRequestLogs.putIfAbsent(user, new LinkedList<>());

        Deque<Long> timestamps = this.userRequestLogs.get(user);

        synchronized (timestamps) {
            // Remove all timestamps older than one minute
            while (!timestamps.isEmpty() && timestamps.peekFirst() < oneMinuteAgo) {
                timestamps.removeFirst();
            }

            if (timestamps.size() < requestsPerMinute) {
                timestamps.addLast(currentTime);
                return true;
            } else {
                return false;
            }
        }

    }
}

// ------------------------------------------------------------------------------------------

class TokenBucket {

    private final long capacity;
    private final double refillRatePerMillis; // tokens per millisecond
    private long lastRefillTimestamp; // In milliseconds
    private final AtomicLong tokens;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucket(long capacity, int refillRatePerMinute) {
        this.capacity = capacity;
        // Convert refill rate from per minute to per millisecond for more granular
        // calculations
        this.refillRatePerMillis = (double) refillRatePerMinute / (60 * 1000); // 60000 ms in a minute
        this.tokens = new AtomicLong(capacity); // Start with a full bucket
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    /**
     * Attempts to consume one token from the bucket.
     * The bucket is refilled based on elapsed time before attempting to consume.
     *
     * @return true if a token was successfully consumed (request is allowed), false
     *         otherwise.
     */
    public boolean tryConsume() {
        lock.lock(); // Acquire lock for thread safety
        try {
            this.refillTokens(); // Refill before checking and consuming
            if (this.tokens.get() > 0) {
                this.tokens.decrementAndGet();
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refills the tokens in the bucket based on the time elapsed since the last
     * refill.
     * This method is called internally before attempting to consume a token.
     */
    private void refillTokens() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRefill = currentTime - lastRefillTimestamp;

        // Only refill if some time has passed
        if (timeSinceLastRefill > 0) {
            // Calculate tokens to add based on elapsed time and refill rate
            long tokensToAdd = (long) (timeSinceLastRefill / this.refillRatePerMillis);
            if (tokensToAdd > 0) {
                // Atomically update tokens, ensuring it doesn't exceed capacity
                this.tokens.updateAndGet(currentTokens -> Math.min(this.capacity, currentTokens + tokensToAdd));
                this.lastRefillTimestamp = currentTime;// Update last refill time
            }
        }
    }

}

class TokenBucketRateLimiter {

    private final int capacity;
    private final int refillRatePerMinute;
    private final ConcurrentHashMap<User, TokenBucket> userBuckets;

    public TokenBucketRateLimiter(int capacity, int refillRatePerMinute) {
        this.capacity = capacity;
        this.refillRatePerMinute = refillRatePerMinute;
        this.userBuckets = new ConcurrentHashMap<>();
    }

    public boolean isAllowed(User user) {
        TokenBucket userBucket = this.userBuckets
                .computeIfAbsent(user, k -> new TokenBucket(this.capacity, this.refillRatePerMinute));

        return userBucket.tryConsume();
    }

}

// ------------------------------------------------------------------------------------------

// A simple marker class for our "requests"
class Request {

    public long timestamp = System.currentTimeMillis();

}

/**
 * Implements the core Leaky Bucket algorithm.
 * Requests are placed in a queue (the bucket) and processed at a fixed,
 * constant rate.
 */
class LeakyBucket {

    private final BlockingQueue<Request> bucket;
    private final ScheduledExecutorService scheduler;

    /**
     * Constructs a Leaky Bucket.
     *
     * @param capacity          The maximum number of requests the bucket can hold.
     * @param leakRatePerSecond The rate at which requests are processed, in
     *                          requests per second.
     */
    public LeakyBucket(int capacity, int leakRatePerSecond) {
        this.bucket = new LinkedBlockingQueue<>(capacity);

        // Schedule a task to "leak" a request at a fixed rate.
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        long leakIntervalMillis = 1000 / leakRatePerSecond;
        this.scheduler.scheduleAtFixedRate(this::leakRequest, 0, leakIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Tries to add a new request to the bucket.
     *
     * @return true if the request was added, false if the bucket is full.
     */
    public boolean tryAddRequest() {
        // offer() returns false if the queue is full (bucket overflows)
        return this.bucket.offer(new Request());
    }

    /**
     * Processes a single request from the bucket. This is the "leak" operation.
     */
    private void leakRequest() {
        Request request = this.bucket.poll();
        if (request != null) {
            // In a real-world scenario, you would process the request here.
            // For this example, we just print a message.
            long latency = System.currentTimeMillis() - request.timestamp;
            // System.out.println("Processed a request after " + latency + "ms. Bucket size:
            // " + bucket.size());
        }
    }

    // Don't forget to shut down the scheduler when the application closes.
    public void shutdown() {
        scheduler.shutdown();
    }

}

class LeakyBucketRateLimiter {

    private final int defaultCapacity;
    private final int defaultLeakRatePerSecond;
    private final ConcurrentHashMap<User, LeakyBucket> userBuckets;

    public LeakyBucketRateLimiter(int defaultCapacity, int defaultLeakRatePerSecond) {
        this.defaultCapacity = defaultCapacity;
        this.defaultLeakRatePerSecond = defaultLeakRatePerSecond;
        this.userBuckets = new ConcurrentHashMap<>();
    }

    /**
     * Checks if a new request from the user can be added to their bucket.
     *
     * @param userId The ID of the user making the request.
     * @return true if the request can be added (not rate limited), false otherwise.
     */
    public boolean isAllowed(User user) {
        LeakyBucket userBucket = this.userBuckets
                .computeIfAbsent(user, k -> new LeakyBucket(this.defaultCapacity, this.defaultLeakRatePerSecond));

        return userBucket.tryAddRequest();
    }

}
