import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class LogSystem {

    /*
     * STEP 1: Choose a simple representation for each log.
     *
     * Each log consists of:
     *   - its unique ID
     *   - its timestamp
     *
     * A record is convenient in modern Java because it gives us an immutable
     * data carrier with an ID accessor and a timestamp accessor.
     *
     * We keep timestamps as Strings. There is no need to parse them into
     * date-time objects because the problem guarantees a fixed-width format.
     */
    private record Log(int id, String timestamp) {}

    /*
     * STEP 2: Store all logs.
     *
     * An ArrayList is sufficient because:
     *   - put() only needs to append a log.
     *   - retrieve() can inspect every log.
     *
     * We do not need sorting, a tree, or a hash map for the basic solution.
     */
    private final List<Log> logs = new ArrayList<>();

    /*
     * STEP 3: Convert granularity into a prefix length.
     *
     * Timestamp format:
     *
     *   YYYY:MM:DD:HH:MM:SS
     *   0123456789012345678  <- character positions
     *
     * Prefix lengths:
     *
     *   Year   ->  4 characters: YYYY
     *   Month  ->  7 characters: YYYY:MM
     *   Day    -> 10 characters: YYYY:MM:DD
     *   Hour   -> 13 characters: YYYY:MM:DD:HH
     *   Minute -> 16 characters: YYYY:MM:DD:HH:MM
     *   Second -> 19 characters: YYYY:MM:DD:HH:MM:SS
     *
     * We include the colons that occur between the components.
     *
     * Example:
     *   "2017:01:02:09:30:00".substring(0, 10)
     *   returns "2017:01:02".
     */
    private static final Map<String, Integer> PREFIX_LENGTHS = Map.of(
        "Year", 4,
        "Month", 7,
        "Day", 10,
        "Hour", 13,
        "Minute", 16,
        "Second", 19
    );

    /*
     * STEP 4: Initialize an empty logging system.
     *
     * ArrayList is already initialized when the object is constructed.
     */
    public LogSystem() {
    }

    /*
     * STEP 5: Insert a log.
     *
     * We do not need to sort the list or compare the timestamp here.
     * Simply retain the ID and its original timestamp.
     *
     * Time complexity: O(1) amortized.
     * Space complexity: O(1) additional space per inserted log.
     */
    public void put(int id, String timestamp) {
        logs.add(new Log(id, timestamp));
    }

    /*
     * STEP 6: Retrieve all logs within the requested range.
     *
     * The main idea:
     *
     *   A. Determine how many timestamp characters matter.
     *   B. Extract that prefix from start and end.
     *   C. Extract the same prefix from every stored log.
     *   D. Include a log if its prefix lies between the two boundaries.
     */
    public List<Integer> retrieve(
            String start,
            String end,
            String granularity) {

        /*
         * A. Find the required precision.
         *
         * If granularity is "Day", length is 10.
         * If granularity is "Month", length is 7.
         *
         * The problem guarantees a valid granularity.
         */
        int length = PREFIX_LENGTHS.get(granularity);

        /*
         * B. Reduce the query boundaries to the requested precision.
         *
         * Example:
         *
         * start = "2017:01:01:12:00:00"
         * end   = "2017:01:03:08:00:00"
         * granularity = "Day"
         *
         * We get:
         *
         * startPrefix = "2017:01:01"
         * endPrefix   = "2017:01:03"
         *
         * Notice that the hours, minutes, and seconds are discarded.
         */
        String startPrefix = start.substring(0, length);
        String endPrefix = end.substring(0, length);

        /*
         * C. Create a list for the matching IDs.
         *
         * It is initially empty. We append IDs only when their logs match.
         */
        List<Integer> result = new ArrayList<>();

        /*
         * D. Examine every stored log.
         *
         * This is a linear scan. We deliberately choose the simplest
         * implementation first rather than adding an index prematurely.
         */
        for (Log log : logs) {

            /*
             * Extract the same part of this log's timestamp.
             *
             * If granularity is Day, the comparison ignores the time of day.
             */
            String logPrefix = log.timestamp().substring(0, length);

            /*
             * Check the inclusive range:
             *
             *   logPrefix >= startPrefix
             *   AND
             *   logPrefix <= endPrefix
             *
             * String.compareTo() is suitable because the timestamp fields
             * are fixed-width, zero-padded, and arranged from largest unit
             * (year) to smallest unit (second).
             *
             * compareTo() returns:
             *   a negative number if the left string comes before the right
             *   zero if the strings are equal
             *   a positive number if the left string comes after the right
             *
             * Both comparisons include equality, so logs at either endpoint
             * are included.
             */
            boolean isAfterOrAtStart =
                    logPrefix.compareTo(startPrefix) >= 0;

            boolean isBeforeOrAtEnd =
                    logPrefix.compareTo(endPrefix) <= 0;

            /*
             * Only logs satisfying BOTH conditions belong to the answer.
             *
             * Example:
             *   startPrefix = "2017:01:01"
             *   logPrefix   = "2017:01:02"
             *   endPrefix   = "2017:01:03"
             *
             * Both conditions are true, so the ID is included.
             */
            if (isAfterOrAtStart && isBeforeOrAtEnd) {
                result.add(log.id());
            }
        }

        /*
         * Return every matching ID.
         *
         * The problem does not require a particular order, so there is no
         * need to sort the result.
         */
        return result;
    }

    /*
     * COMPLEXITY ANALYSIS
     *
     * Let:
     *   n = number of stored logs
     *   r = number of matching logs
     *   L = maximum timestamp length, which is 19
     *
     * put():
     *   Amortized O(1) time per insertion.
     *
     * retrieve():
     *   O(n * L) when counting character operations, because we examine each
     *   log and extract/compare prefixes of at most 19 characters.
     *   Since L is a fixed constant, this simplifies to O(n).
     *
     *   Constructing the returned list takes O(r) additional time.
     *   The full time bound remains O(n), because r <= n.
     *
     * Persistent storage:
     *   O(n) for all stored logs.
     *
     * Additional space per retrieve():
     *   O(r) for the returned IDs.
     *   The two boundary prefixes require O(L) space, which is constant.
     *
     * WHY THIS SOLUTION IS A GOOD START:
     *   It directly expresses the problem's requirements, handles all
     *   granularities uniformly, and avoids unnecessary date calculations.
     */
}










































import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

class LogSystem {

    // A timestamp may have several IDs, so retain a list for each key.
    private final NavigableMap<String, List<Integer>> logs = new TreeMap<>();

    // Number of timestamp components that matter at each granularity.
    private static final Map<String, Integer> COMPONENTS = Map.of(
        "Year", 1,
        "Month", 2,
        "Day", 3,
        "Hour", 4,
        "Minute", 5,
        "Second", 6
    );

    public LogSystem() {}

    public void put(int id, String timestamp) {
        // Preserve all IDs when multiple logs have the same timestamp.
        logs.computeIfAbsent(timestamp, key -> new ArrayList<>()).add(id);
    }

    public List<Integer> retrieve(
            String start,
            String end,
            String granularity) {

        int components = COMPONENTS.get(granularity);

        // Keep the meaningful prefix and fill every smaller component with 00.
        // Example at Month granularity:
        // 2017:03 becomes 2017:03:00:00:00:00.
        String lower = start.substring(0, prefixLength(components))
                + ":00".repeat(6 - components);

        // Fill smaller components with 99 to include every possible timestamp
        // in the ending unit. These are lexicographical sentinels, not dates.
        // Example at Month granularity:
        // 2017:06 becomes 2017:06:99:99:99:99.
        String upper = end.substring(0, prefixLength(components))
                + ":99".repeat(6 - components);

        List<Integer> result = new ArrayList<>();

        // TreeMap locates the range and iterates only matching timestamp keys.
        // Both bounds are inclusive.
        for (List<Integer> ids : logs.subMap(lower, true, upper, true).values()) {
            result.addAll(ids);
        }

        return result;
    }

    private static int prefixLength(int components) {
        // Each component contributes two colons except the first component,
        // the year. For six components, the length is 19.
        return components * 3 + 1;
    }

    /*
     * Time complexity:
     *
     * put:
     *   O(log m) to insert into the TreeMap, plus amortized O(1) to append
     *   an ID to its list, where m is the number of distinct timestamps.
     *
     * retrieve:
     *   O(log m + k + r), where k is the number of matching timestamp keys
     *   and r is the total number of IDs returned.
     *
     * Space:
     *   O(n) for stored logs and O(r) for the returned list.
     *
     * The prefix-scan solution is simpler and sufficient for the original
     * constraints. This version is useful when range queries become frequent.
     */
}



