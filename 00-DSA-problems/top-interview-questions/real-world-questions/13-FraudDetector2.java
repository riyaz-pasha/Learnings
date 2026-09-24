import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Given a stream of transactions, detect potential fraud patterns such as:
1) High-value transactions
2) Rapid repeated transactions by same user
3) Same IP used by many users in short time

This is a REAL-TIME STREAMING PROBLEM.

================================================================================
INTERVIEW FLOW (WHAT I SAY OUT LOUD)
--------------------------------------------------------------------------------
Step 1: Clarify
- Transactions arrive continuously
- Fraud detection must be near real-time
- Rules are deterministic (not ML)
- Time-based patterns matter

Step 2: Key Insight
- We must maintain STATE across events
- Sliding window logic is required
- HashMaps + Deques are ideal

Step 3: Design
- Each fraud rule has its own tracker
- Each transaction is evaluated against all rules
- If any rule triggers → flag transaction

================================================================================
*/

class Transaction {
    String transactionId;
    String userId;
    String ip;
    double amount;
    long timestamp; // epoch millis

    Transaction(String transactionId, String userId, String ip, double amount, long timestamp) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.ip = ip;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}

class FraudDetector {

    /* ========================= CONFIGURATION ========================= */

    private static final double HIGH_VALUE_THRESHOLD = 10_000.0;

    private static final int MAX_TX_PER_USER = 3;
    private static final long USER_WINDOW_MS = 60_000; // 1 minute

    private static final int MAX_USERS_PER_IP = 3;
    private static final long IP_WINDOW_MS = 60_000; // 1 minute

    /* ========================= STATE ========================= */

    // user → timestamps of recent transactions
    private final Map<String, Deque<Long>> userTxTimestamps = new HashMap<>();

    // ip → user → timestamps
    private final Map<String, Map<String, Deque<Long>>> ipUserMap = new HashMap<>();

    /* ========================= ENTRY POINT ========================= */

    public boolean isFraudulent(Transaction tx) {

        if (isHighValue(tx)) {
            return true;
        }

        if (isRapidRepeatUser(tx)) {
            return true;
        }

        if (isSuspiciousIP(tx)) {
            return true;
        }

        return false;
    }

    /* ========================= RULE 1 ========================= */

    /*
     * Rule:
     * Transaction amount exceeds threshold
     */
    private boolean isHighValue(Transaction tx) {
        return tx.amount > HIGH_VALUE_THRESHOLD;
    }

    /* ========================= RULE 2 ========================= */

    /*
     * Rule:
     * Same user makes many transactions in a short time window
     */
    private boolean isRapidRepeatUser(Transaction tx) {

        userTxTimestamps.putIfAbsent(tx.userId, new ArrayDeque<>());
        Deque<Long> timestamps = userTxTimestamps.get(tx.userId);

        cleanupOld(timestamps, tx.timestamp, USER_WINDOW_MS);
        timestamps.addLast(tx.timestamp);

        return timestamps.size() >= MAX_TX_PER_USER;
    }

    /* ========================= RULE 3 ========================= */

    /*
     * Rule:
     * Same IP used by multiple users within short window
     */
    private boolean isSuspiciousIP(Transaction tx) {

        ipUserMap.putIfAbsent(tx.ip, new HashMap<>());
        Map<String, Deque<Long>> users = ipUserMap.get(tx.ip);

        users.putIfAbsent(tx.userId, new ArrayDeque<>());
        Deque<Long> timestamps = users.get(tx.userId);

        cleanupOld(timestamps, tx.timestamp, IP_WINDOW_MS);
        timestamps.addLast(tx.timestamp);

        int activeUsers = 0;
        for (Deque<Long> q : users.values()) {
            cleanupOld(q, tx.timestamp, IP_WINDOW_MS);
            if (!q.isEmpty()) {
                activeUsers++;
            }
        }

        return activeUsers >= MAX_USERS_PER_IP;
    }

    /* ========================= HELPERS ========================= */

    /*
     * Sliding window eviction
     */
    private void cleanupOld(Deque<Long> deque, long currentTime, long window) {
        while (!deque.isEmpty() && currentTime - deque.peekFirst() > window) {
            deque.pollFirst();
        }
    }

    /* ========================= DEMO ========================= */

    public static void main(String[] args) {

        FraudDetector detector = new FraudDetector();
        long now = System.currentTimeMillis();

        Transaction t1 = new Transaction("t1", "user1", "1.1.1.1", 500, now);
        Transaction t2 = new Transaction("t2", "user1", "1.1.1.1", 600, now + 1000);
        Transaction t3 = new Transaction("t3", "user1", "1.1.1.1", 700, now + 2000);

        System.out.println(detector.isFraudulent(t1)); // false
        System.out.println(detector.isFraudulent(t2)); // false
        System.out.println(detector.isFraudulent(t3)); // true (rapid repeat)
    }
}

/*
================================================================================
TIME & SPACE COMPLEXITY (INTERVIEW ANSWER)
--------------------------------------------------------------------------------
Let:
N = number of transactions in window
U = number of users
I = number of IPs

Per transaction:
- Rule checks: O(1)
- Sliding window cleanup: amortized O(1)

Overall:
TIME:  O(1) per transaction (amortized)
SPACE: O(N) within sliding windows

================================================================================
FOLLOW-UP DISCUSSION (WHAT TO SAY)
--------------------------------------------------------------------------------
1) More rules?
   → Add more independent detectors

2) Distributed system?
   → Partition by userId or IP
   → Use stream processors (Kafka/Flink)

3) ML-based fraud?
   → Rule-based pre-filter + ML re-ranking

================================================================================
ONE-LINE SUMMARY (MEMORIZE)
--------------------------------------------------------------------------------
"Fraud detection in streams is best handled using stateful sliding windows with
hash-based lookups and rule evaluation."

================================================================================
*/
