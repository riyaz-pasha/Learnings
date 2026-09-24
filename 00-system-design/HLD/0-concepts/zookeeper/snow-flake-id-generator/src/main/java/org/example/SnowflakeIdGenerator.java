package org.example;

public class SnowflakeIdGenerator {

    private final static long EPOCH = 1700000000000L; // Custom epoch (ms)

    private final static long WORKER_ID_BITS = 10L;
    private final static long SEQUENCE_BITS = 12L;

    private final static long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private final static long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private final static long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID out of range");
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long currentTime = this.currentTime();

        if (currentTime < this.lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID");
        }

        if (currentTime == this.lastTimestamp) {
            this.sequence = (this.sequence + 1) & MAX_SEQUENCE;
            if (this.sequence == 0) {
                currentTime = this.waitNextMillis(this.lastTimestamp);
            }
        } else {
            this.sequence = 0L;
        }

        this.lastTimestamp = currentTime;

        return ((currentTime - EPOCH) << TIMESTAMP_SHIFT)
                | (this.workerId << WORKER_ID_SHIFT)
                | this.sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long currentTime = this.currentTime();
        while (currentTime <= lastTimestamp) {
            currentTime = this.currentTime();
        }
        return currentTime;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }

}
