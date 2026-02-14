import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

/*
 * Event Deduplicator with Session Expiry
 *
 * Problem:
 * - For each user, detect if an eventType is duplicate within SESSION_TIMEOUT.
 * - If user is inactive for SESSION_TIMEOUT, session expires and we forget past events.
 *
 * Example:
 * timeout = 5 min
 *
 * user A:
 *  t=1000   click  -> not duplicate
 *  t=2000   click  -> duplicate (within 5 min)
 *  t=400000 click  -> not duplicate (session expired, old events cleared)
 *
 * Time Complexity:
 * - Each call does O(1) HashMap operations (average).
 * - O(1) per request.
 *
 * Space Complexity:
 * - O(U * E) where:
 *     U = number of active users
 *     E = number of eventTypes tracked per user session
 */
class EventDeduplicator {

    private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    private static class UserSession {

        // last time user performed ANY event
        long lastActivityTime;

        // eventType -> lastSeenTimestamp
        Map<String, Long> eventLastSeen = new HashMap<>();

        public UserSession(long timestamp) {
            this.lastActivityTime = timestamp;
        }

        public void resetSession(long timestamp) {
            eventLastSeen.clear();
            lastActivityTime = timestamp;
        }

        public void updateEvent(String eventType, long timestamp) {
            eventLastSeen.put(eventType, timestamp);
            lastActivityTime = timestamp;
        }
    }

    private final Map<String, UserSession> sessions = new HashMap<>();

    public boolean isDuplicate(long timestamp, String userId, String eventType) {

        UserSession session = sessions.get(userId);

        // If session doesn't exist, create a new session.
        if (session == null) {
            session = new UserSession(timestamp);
            sessions.put(userId, session);

            // First time event is always NOT duplicate
            session.updateEvent(eventType, timestamp);
            return false;
        }

        /*
         * If user has been inactive beyond timeout,
         * treat this as a NEW session.
         *
         * Meaning:
         * - We forget old events
         * - Duplicate detection starts fresh
         */
        if (timestamp - session.lastActivityTime > SESSION_TIMEOUT) {
            session.resetSession(timestamp);
        }

        Long lastSeen = session.eventLastSeen.get(eventType);

        boolean duplicate =
                (lastSeen != null && timestamp - lastSeen <= SESSION_TIMEOUT);

        // Update last seen for this eventType
        session.updateEvent(eventType, timestamp);

        return duplicate;
    }
}


/*
 * Event Deduplicator with Expiring Sessions (Heap-based Eviction)
 *
 * Goal:
 * - Detect duplicate events within a user session window (SESSION_TIMEOUT).
 * - Automatically evict expired user sessions to prevent memory growth.
 *
 * Definitions:
 * - A session is active if the user has performed ANY event within last 5 minutes.
 * - An event is duplicate if the same eventType was seen within last 5 minutes.
 *
 * Data Structures:
 * 1) sessions: Map<userId, UserSession>
 *      - Stores per-user session state.
 *
 * 2) expiryHeap: MinHeap ordered by expiry time
 *      - Each time user activity happens, we push a new ExpiryEntry.
 *      - We do "lazy deletion" because old heap entries may become stale.
 *
 * Why heap?
 * - So we can evict oldest sessions efficiently.
 * - Without heap, we'd need to scan all sessions (O(U)).
 *
 * Time Complexity:
 * - Each isDuplicate() call:
 *      O(log U) due to heap insert
 *      + eviction loop (amortized O(log U) per session removed)
 *
 * Overall amortized: O(log U) per event.
 *
 * Space Complexity:
 * - sessions map: O(U * E)  (U users, E eventTypes per user)
 * - heap: O(U) (but can temporarily grow due to stale entries)
 */
class EventDeduplicator2 {

    private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    /*
     * Stores per-user session state.
     */
    private static class UserSession {
        long lastActivityTime;
        Map<String, Long> eventLastSeen = new HashMap<>();

        UserSession(long timestamp) {
            this.lastActivityTime = timestamp;
        }
    }

    /*
     * Heap entry that tells when a user session should expire.
     *
     * expiryTime = lastActivityTime + SESSION_TIMEOUT
     *
     * IMPORTANT:
     * We store lastActivityTimeSnapshot because heap entries can become stale.
     */
    private static class ExpiryEntry {
        String userId;
        long expiryTime;
        long lastActivityTimeSnapshot;

        ExpiryEntry(String userId, long expiryTime, long lastActivityTimeSnapshot) {
            this.userId = userId;
            this.expiryTime = expiryTime;
            this.lastActivityTimeSnapshot = lastActivityTimeSnapshot;
        }
    }

    // userId -> session state
    private final Map<String, UserSession> sessions = new HashMap<>();

    // MinHeap based on expiryTime (oldest expiry comes first)
    private final PriorityQueue<ExpiryEntry> expiryHeap =
            new PriorityQueue<>(Comparator.comparingLong(e -> e.expiryTime));

    /*
     * Main API:
     * Returns true if this event is duplicate within session timeout.
     */
    public boolean isDuplicate(long timestamp, String userId, String eventType) {

        // Step 1: cleanup expired sessions before processing current event
        cleanupExpiredSessions(timestamp);

        // Step 2: get or create session
        UserSession session = sessions.get(userId);

        if (session == null) {
            session = new UserSession(timestamp);
            sessions.put(userId, session);
        }

        // Step 3: check if this eventType was seen recently
        Long lastSeen = session.eventLastSeen.get(eventType);

        boolean duplicate =
                (lastSeen != null && timestamp - lastSeen <= SESSION_TIMEOUT);

        // Step 4: update eventType timestamp + lastActivityTime
        session.eventLastSeen.put(eventType, timestamp);
        session.lastActivityTime = timestamp;

        // Step 5: push expiry entry into heap
        expiryHeap.offer(new ExpiryEntry(
                userId,
                timestamp + SESSION_TIMEOUT,
                timestamp
        ));

        return duplicate;
    }

    /*
     * Cleanup logic (Lazy eviction using heap)
     *
     * We remove sessions that have expired based on their last activity.
     *
     * Example:
     * - User A lastActivity=100
     * - expiryTime=400
     * - If current timestamp=500, session should be removed.
     *
     * But we must be careful:
     * - Heap may contain stale entries for the same user.
     *   Example:
     *     user activity at t=100 -> heap entry expiry=400
     *     user activity at t=300 -> heap entry expiry=600
     *
     * When we pop expiry=400 entry, we must verify:
     * - does user's current lastActivityTime still match that old snapshot?
     * If not, skip it (stale entry).
     */
    private void cleanupExpiredSessions(long timestamp) {

        while (!expiryHeap.isEmpty()) {

            ExpiryEntry entry = expiryHeap.peek();

            // If earliest expiry is still in future, stop cleanup
            if (entry.expiryTime > timestamp) {
                return;
            }

            expiryHeap.poll();

            UserSession session = sessions.get(entry.userId);

            // session might already be removed
            if (session == null) {
                continue;
            }

            /*
             * Stale entry check:
             *
             * If session.lastActivityTime != snapshot,
             * it means the user had more recent activity,
             * so this heap entry is outdated and should be ignored.
             */
            if (session.lastActivityTime != entry.lastActivityTimeSnapshot) {
                continue;
            }

            // At this point, session is truly expired
            sessions.remove(entry.userId);
        }
    }
}



/*
 * Event Deduplicator with Session TTL Eviction (TreeMap version)
 *
 * Problem:
 * - Each user has a session window of SESSION_TIMEOUT (5 minutes).
 * - If user is inactive for SESSION_TIMEOUT, session expires.
 * - An event is duplicate if same eventType occurs within SESSION_TIMEOUT.
 *
 * Data Structures:
 * 1) sessions:
 *      userId -> UserSession
 *
 * 2) expiryMap (TreeMap):
 *      expiryTime -> Set of userIds expiring at that exact time
 *
 * Why TreeMap?
 * - TreeMap keeps keys sorted.
 * - We can efficiently evict all sessions with expiryTime <= currentTimestamp.
 *
 * Time Complexity:
 * - cleanupExpiredSessions(): O(number of expired users * log U)
 * - each isDuplicate(): O(log U) due to TreeMap insert/delete
 *
 * Space Complexity:
 * - O(U * E) for sessions
 * - O(U) for expiryMap
 */
class EventDeduplicatorTreeMap {

    private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    private static class UserSession {
        long lastActivityTime;
        long expiryTime;

        // eventType -> lastSeenTime
        Map<String, Long> eventLastSeen = new HashMap<>();

        UserSession(long timestamp) {
            this.lastActivityTime = timestamp;
            this.expiryTime = timestamp + SESSION_TIMEOUT;
        }
    }

    // userId -> session
    private final Map<String, UserSession> sessions = new HashMap<>();

    // expiryTime -> all users expiring at that time
    private final TreeMap<Long, Set<String>> expiryMap = new TreeMap<>();

    /*
     * Returns true if (userId, eventType) is duplicate within session timeout.
     */
    public boolean isDuplicate(long timestamp, String userId, String eventType) {

        // Step 1: Evict expired sessions before processing this request
        cleanupExpiredSessions(timestamp);

        // Step 2: Get or create user session
        UserSession session = sessions.get(userId);

        if (session == null) {
            session = new UserSession(timestamp);
            sessions.put(userId, session);

            // Add to expiryMap
            addUserToExpiryMap(userId, session.expiryTime);

            // First time event is never duplicate
            session.eventLastSeen.put(eventType, timestamp);
            return false;
        }

        // Step 3: Check duplicate condition for this eventType
        Long lastSeen = session.eventLastSeen.get(eventType);

        boolean duplicate =
                (lastSeen != null && timestamp - lastSeen <= SESSION_TIMEOUT);

        // Step 4: Update session state
        session.eventLastSeen.put(eventType, timestamp);

        // Since user is active now, session expiry moves forward
        updateSessionExpiry(userId, session, timestamp);

        return duplicate;
    }

    /*
     * Removes all expired sessions.
     *
     * TreeMap allows us to fetch all expiry keys <= timestamp.
     */
    private void cleanupExpiredSessions(long timestamp) {

        while (!expiryMap.isEmpty()) {

            long earliestExpiry = expiryMap.firstKey();

            // earliest session hasn't expired yet -> stop
            if (earliestExpiry > timestamp) {
                return;
            }

            // Remove all users expiring at earliestExpiry
            Set<String> expiringUsers = expiryMap.pollFirstEntry().getValue();

            for (String userId : expiringUsers) {

                UserSession session = sessions.get(userId);

                /*
                 * Safety check:
                 * User might have been updated to a later expiryTime,
                 * but still existed in old expiry bucket.
                 */
                if (session != null && session.expiryTime == earliestExpiry) {
                    sessions.remove(userId);
                }
            }
        }
    }

    /*
     * Updates session expiry time when user becomes active.
     *
     * This requires:
     * - removing user from old expiry bucket
     * - adding user to new expiry bucket
     */
    private void updateSessionExpiry(String userId, UserSession session, long timestamp) {

        long oldExpiry = session.expiryTime;

        // remove from old expiry bucket
        removeUserFromExpiryMap(userId, oldExpiry);

        // update expiry values
        session.lastActivityTime = timestamp;
        session.expiryTime = timestamp + SESSION_TIMEOUT;

        // add into new expiry bucket
        addUserToExpiryMap(userId, session.expiryTime);
    }

    private void addUserToExpiryMap(String userId, long expiryTime) {
        expiryMap.computeIfAbsent(expiryTime, x -> new HashSet<>()).add(userId);
    }

    private void removeUserFromExpiryMap(String userId, long expiryTime) {

        Set<String> bucket = expiryMap.get(expiryTime);

        if (bucket == null) return;

        bucket.remove(userId);

        // if bucket becomes empty, remove expiry key completely
        if (bucket.isEmpty()) {
            expiryMap.remove(expiryTime);
        }
    }
}
