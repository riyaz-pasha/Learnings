import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.TreeMap;

class Event {

    int id;
    long start;
    long end;

    Event(int id, long start, long end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

}

class OverlappingEventsFinder {

    /*
     * Time: O(N log N) — due to sorting and heap operations
     * Space: O(N) — heap and result list
     */
    public List<int[]> findOverlappingEvents(List<Event> events) {
        List<int[]> result = new ArrayList<>();

        if (events == null || events.size() < 2) {
            return result;
        }

        // Sort events by start time O(N log N)
        events.sort(Comparator.comparingLong(e -> e.start));

        // Min-heap to store events by their end time
        PriorityQueue<Event> active = new PriorityQueue<>(Comparator.comparingLong(e -> e.end));

        /*
         * For each event:
         * - Remove all events from min-heap whose end <= current.start → amortized O(N)
         * over all events.
         * - For all events left in heap (say K active events), add overlapping pairs →
         * total overlap checks across all events = O(M) where M = number of overlapping
         * pairs
         * - Insert current event in heap → O(log N)
         */
        for (Event event : events) {
            // Remove all events that have ended
            while ((!active.isEmpty() && active.peek().end <= event.start)) {
                active.poll();
            }

            // All remaining events in heap overlap with current
            for (Event e : active) {
                result.add(new int[] { e.id, event.id });
            }

            active.offer(event);
        }

        return result;

    }

    public static void main(String[] args) {
        List<Event> events = Arrays.asList(
                new Event(1, 1, 5),
                new Event(2, 4, 6),
                new Event(3, 7, 9),
                new Event(4, 2, 8));

        OverlappingEventsFinder finder = new OverlappingEventsFinder();
        List<int[]> overlaps = finder.findOverlappingEvents(events);

        for (int[] pair : overlaps) {
            System.out.println("Overlapping pair: Event " + pair[0] + " & Event " + pair[1]);
        }
        /*
         * Overlapping pair: Event 1 & Event 2
         * Overlapping pair: Event 1 & Event 4
         * Overlapping pair: Event 2 & Event 4
         */
    }

}

class EventStreamProcessor {

    // Store past events (sorted by start time)
    TreeMap<Long, List<Event>> timeline = new TreeMap<>();

    /*
     * Time per event: O(log N + K) where:
     * - log N to find overlapping intervals via TreeMap
     * - K is the number of overlaps
     * 
     * Space: O(N) for storing past events
     */
    public List<Event> process(Event newEvent) {
        List<Event> overlaps = new ArrayList<>();

        NavigableMap<Long, List<Event>> allEventsBeforeThisEventEnds = this.timeline.headMap(newEvent.end, false);
        for (Map.Entry<Long, List<Event>> entry : allEventsBeforeThisEventEnds.entrySet()) {
            for (Event e : entry.getValue()) {
                if (e.end > newEvent.start) {
                    overlaps.add(e);
                }
            }
        }

        // Store this event in the timeline
        timeline.computeIfAbsent(newEvent.start, k -> new ArrayList<>()).add(newEvent);

        return overlaps;
    }

    /*
     * 🔍 Steps per event:
     * timeline.headMap(newEvent.end, false) → O(log N) for TreeMap operation (range
     * lookup by key)
     * 
     * Loop through returned events and check if e.end > newEvent.start → up to O(K)
     * where K = overlapping events
     * 
     * Insert event into TreeMap → O(log N)
     * 
     * ✅ Time Complexity per event:
     * O(log N + K) (where K is number of overlapping events found)
     * So, for N events total:
     * O(N log N + total_K) where total_K = total overlaps found across all events
     * 
     * ✅ Space Complexity:
     * O(N) for TreeMap storing all past events.
     */

}
