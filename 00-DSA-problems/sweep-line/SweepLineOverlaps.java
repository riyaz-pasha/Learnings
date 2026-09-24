import java.util.ArrayList;
import java.util.List;

class SweepLineOverlaps {
    public int maxOverlaps(int[][] intervals) {
        List<int[]> events = new ArrayList<>();

        for (int[] interval : intervals) {
            events.add(new int[] { interval[0], 1 }); // start of interval
            events.add(new int[] { interval[1], -1 }); // end of interval
        }

        events.sort((a, b) -> {
            if (a[0] == b[0]) {
                return a[1] - b[1]; // end(-1) before start(1)
            }
            return a[0] - b[0];
        });

        int maxOverlap = 0, currentOverlap = 0;
        for (int[] event : events) {
            currentOverlap += event[1];
            maxOverlap = Math.max(maxOverlap, currentOverlap);
        }
        return maxOverlap;
    }
}
