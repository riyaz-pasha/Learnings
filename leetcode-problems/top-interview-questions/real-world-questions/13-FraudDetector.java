import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

class Transaction {
    private String transactionId;
    private String userId;
    private double amount;
    private String ipAddress;
    private LocalDateTime timestamp;

    public Transaction(String transactionId, String userId, double amount, String ipAddress) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getTransactionId() {
        return transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", ip='" + ipAddress + '\'' +
                ", time=" + timestamp +
                '}';
    }

}

class StatefulProcessor {

    // Map to store recent transactions by IP address for "same IP" detection
    // Key: IP Address, Value: Queue of transactions
    private Map<String, Queue<Transaction>> ipTransactionHistory;

    // Map to store the last transaction timestamp for each user for "fast repeat"
    // detection
    // Key: User ID, Value: Last transaction timestamp
    private Map<String, LocalDateTime> userLastTransactionTime;

    // Thresholds
    private static final int SAME_IP_THRESHOLD = 5; // e.g., more than 5 transactions from a single IP
    private static final int FAST_REPEAT_SECONDS = 30; // e.g., less than 30 seconds between transactions
    private static final double HIGH_VALUE_THRESHOLD = 1000.0; // e.g., over $1000

    public StatefulProcessor() {
        this.ipTransactionHistory = new HashMap<>();
        this.userLastTransactionTime = new HashMap<>();
    }

    public void processTransaction(Transaction transaction) {
        // Update state for IP-based checks
        ipTransactionHistory.computeIfAbsent(transaction.getIpAddress(), k -> new LinkedList<>()).add(transaction);
        // Clean up old transactions from the queue to only keep recent ones
        cleanUpIpHistory(transaction.getIpAddress(), transaction.getTimestamp());

        // Update state for user-based checks
        userLastTransactionTime.put(transaction.getUserId(), transaction.getTimestamp());
    }

    private void cleanUpIpHistory(String ipAddress, LocalDateTime currentTime) {
        Queue<Transaction> history = ipTransactionHistory.get(ipAddress);
        if (history != null) {
            while (!history.isEmpty() && ChronoUnit.MINUTES.between(history.peek().getTimestamp(), currentTime) > 5) {
                // Keep history for the last 5 minutes
                history.poll();
            }
        }
    }

    public boolean isHighValue(Transaction transaction) {
        return transaction.getAmount() > HIGH_VALUE_THRESHOLD;
    }

    public boolean isFastRepeat(Transaction transaction) {
        String userId = transaction.getUserId();
        if (userLastTransactionTime.containsKey(userId)) {
            LocalDateTime lastTime = userLastTransactionTime.get(userId);
            long secondsBetween = ChronoUnit.SECONDS.between(lastTime, transaction.getTimestamp());
            if (secondsBetween <= FAST_REPEAT_SECONDS) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMultipleUsersFromSameIp(String ipAddress) {
        Queue<Transaction> history = ipTransactionHistory.get(ipAddress);
        if (history == null || history.size() < SAME_IP_THRESHOLD) {
            return false;
        }

        long distinctUsers = history.stream()
                .map(Transaction::getUserId)
                .distinct()
                .count();

        return distinctUsers >= 2;
    }
}

class FraudDetector {

    private StatefulProcessor stateProcessor;

    // Risk score weights
    private static final int HIGH_VALUE_WEIGHT = 50;
    private static final int FAST_REPEAT_WEIGHT = 40;
    private static final int SAME_IP_WEIGHT = 60;
    private static final int FRAUD_THRESHOLD = 60;

    public FraudDetector(StatefulProcessor stateProcessor) {
        this.stateProcessor = stateProcessor;
    }

    public void checkTransaction(Transaction transaction) {
        int riskScore = 0;
        StringBuilder fraudDetails = new StringBuilder();

        // Check for high-value transactions
        if (stateProcessor.isHighValue(transaction)) {
            riskScore += HIGH_VALUE_WEIGHT;
            fraudDetails.append("High value transaction detected. ");
        }

        // Check for fast repeat transactions
        if (stateProcessor.isFastRepeat(transaction)) {
            riskScore += FAST_REPEAT_WEIGHT;
            fraudDetails.append("Fast repeat transaction detected. ");
        }

        // Check for multiple users from the same IP
        if (stateProcessor.hasMultipleUsersFromSameIp(transaction.getIpAddress())) {
            riskScore += SAME_IP_WEIGHT;
            fraudDetails.append("Multiple users from same IP detected. ");
        }

        // Update the state with the new transaction
        stateProcessor.processTransaction(transaction);

        System.out.println("Processing " + transaction);
        System.out.println("Risk Score: " + riskScore);

        if (riskScore >= FRAUD_THRESHOLD) {
            System.out.println("ALERT: Potential Fraud Detected!");
            System.out.println("Reason: " + fraudDetails.toString());
        } else {
            System.out.println("Transaction appears normal.");
        }
        System.out.println("----------------------------------------");
    }

}
