import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket Rate Limiter
 *
 * Example:
 *   capacity = 10 tokens
 *   refillRate = 2 tokens/sec
 *
 * Allows burst up to 10 requests at once,
 * then refills gradually.
 *
 * Time Complexity:
 *   allowRequest(): O(1)
 *
 * Space Complexity:
 *   O(number of users)
 */
class RateLimiter {

    private static class Bucket {
        double tokens;
        long lastRefillTimeMillis;

        Bucket(double tokens, long lastRefillTimeMillis) {
            this.tokens = tokens;
            this.lastRefillTimeMillis = lastRefillTimeMillis;
        }
    }

    private final int capacity;
    private final double refillTokensPerMillis;

    private final ConcurrentHashMap<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    public RateLimiter(int capacity, double refillTokensPerSecond) {
        this.capacity = capacity;
        this.refillTokensPerMillis = refillTokensPerSecond / 1000.0;
    }

    /**
     * Returns true if request is allowed, else false.
     */
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();

        Bucket bucket = userBuckets.computeIfAbsent(
                userId,
                k -> new Bucket(capacity, now)
        );

        synchronized (bucket) {
            refill(bucket, now);

            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }

            return false;
        }
    }

    /**
     * Refill tokens based on elapsed time since last refill.
     */
    private void refill(Bucket bucket, long now) {
        long elapsed = now - bucket.lastRefillTimeMillis;

        if (elapsed <= 0) return;

        double newTokens = elapsed * refillTokensPerMillis;

        bucket.tokens = Math.min(capacity, bucket.tokens + newTokens);
        bucket.lastRefillTimeMillis = now;
    }
}




class SlidingWindowRateLimiter {

    private final int limit;
    private final long windowMillis = 60_000;

    private final Map<String, Deque<Long>> userHits = new HashMap<>();

    public SlidingWindowRateLimiter(int limitPerMinute) {
        this.limit = limitPerMinute;
    }

    public boolean allowRequest(String userId, long timestampMillis) {
        Deque<Long> q = userHits.computeIfAbsent(userId, k -> new ArrayDeque<>());

        long startWindow = timestampMillis - windowMillis;

        while (!q.isEmpty() && q.peekFirst() <= startWindow) {
            q.pollFirst();
        }

        if (q.size() >= limit) {
            return false;
        }

        q.offerLast(timestampMillis);
        return true;
    }
}



class SlidingWindowBucketRateLimiter {

    private static class UserWindow {
        int[] times = new int[60];
        int[] counts = new int[60];
    }

    private final int limitPerMinute;
    private final Map<String, UserWindow> userMap = new HashMap<>();

    public SlidingWindowBucketRateLimiter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public boolean allowRequest(String userId, long timestampMillis) {
        int currentSecond = (int) (timestampMillis / 1000);

        UserWindow window = userMap.computeIfAbsent(userId, k -> new UserWindow());

        // compute total hits in last 60 seconds
        int totalHits = 0;

        for (int i = 0; i < 60; i++) {
            if (currentSecond - window.times[i] < 60) {
                totalHits += window.counts[i];
            }
        }

        if (totalHits >= limitPerMinute) {
            return false;
        }

        // add this request to current bucket
        int idx = currentSecond % 60;

        if (window.times[idx] != currentSecond) {
            window.times[idx] = currentSecond;
            window.counts[idx] = 1;
        } else {
            window.counts[idx]++;
        }

        return true;
    }
}
