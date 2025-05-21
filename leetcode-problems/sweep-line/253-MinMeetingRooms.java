import java.util.ArrayList;
import java.util.List;

class MinMeetingRoomsII {
    public int minMeetingRooms(int[][] intervals) {
        List<int[]> events = new ArrayList<>();

        for (int[] interval : intervals) {
            events.add(new int[] { interval[0], 1 }); // start of the meeting
            events.add(new int[] { interval[1], -1 }); // end of the meeting
        }

        // Sort by time; if same, end (-1) comes before start (+1)
        events.sort((a, b) -> a[0] == b[0] ? a[1] - b[1] : a[0] - b[0]);

        int activeMeetings = 0, maxRooms = 0;
        for (int[] event : events) {
            activeMeetings += event[1];
            maxRooms = Math.max(maxRooms, activeMeetings);
        }
        return maxRooms;
    }
}
