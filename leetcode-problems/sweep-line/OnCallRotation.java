import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class OnCallRotation {

    public List<OnCall> findOnCall(List<Rotation> rotations) {
        List<Event> events = new ArrayList<>();

        for (Rotation rotation : rotations) {
            events.add(new Event(rotation.startTime, rotation.name, true));
            events.add(new Event(rotation.endTime, rotation.name, false));
        }

        Collections.sort(events);

        List<OnCall> result = new ArrayList<>();
        Set<String> onCall = new TreeSet<>();
        Integer prevTime = null;

        for (Event event : events) {
            if (prevTime != null && prevTime != event.time && !onCall.isEmpty()) {
                result.add(new OnCall(prevTime, event.time, new TreeSet<>(onCall)));
            }

            if (event.isStart) {
                onCall.add(event.name);
            } else {
                onCall.remove(event.name);
            }

            prevTime = event.time;
        }

        return result;
    }

    class Event implements Comparable<Event> {

        int time;
        boolean isStart;
        String name;

        public Event(int time, String name, boolean isStart) {
            this.time = time;
            this.isStart = isStart;
            this.name = name;
        }

        @Override
        public int compareTo(Event other) {
            if (this.time == other.time) {
                return Boolean.compare(this.isStart, other.isStart); // end before start
            }
            return this.time - other.time;
        }

    }

    class OnCall {

        int startTime, endTime;
        Set<String> names;

        public OnCall(int startTime, int endTime, Set<String> names) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.names = names;
        }

        @Override
        public String toString() {
            return startTime + " " + endTime + " " + String.join(", ", names);
        }

    }

    class Rotation {

        int startTime, endTime;
        String name;

        public Rotation(String name, int startTime, int endTime) {
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
        }

    }

    public static void main(String[] args) {
        OnCallRotation solution = new OnCallRotation();
        List<Rotation> input = Arrays.asList(
                solution.new Rotation("Abby", 1, 10),
                solution.new Rotation("Ben", 5, 7),
                solution.new Rotation("Carla", 6, 12),
                solution.new Rotation("David", 15, 17));

        List<OnCall> schedule = solution.findOnCall(input);
        for (OnCall onCall : schedule) {
            System.out.println(onCall);
        }
    }

}
