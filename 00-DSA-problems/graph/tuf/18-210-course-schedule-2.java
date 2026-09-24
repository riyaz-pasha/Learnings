import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class CourseSchedule2 {

    public int[] findOrder(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>();
        int[] indegree = new int[numCourses];

        for (int i = 0; i < numCourses; i++) {
            adj.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            int course = prereq[0];
            int prerequisite = prereq[1];
            adj.get(prerequisite).add(course);
            indegree[course]++;
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int course = 0; course < numCourses; course++) {
            if (indegree[course] == 0) {
                queue.offer(course);
            }
        }

        int[] result = new int[numCourses];
        int index = 0;
        while (!queue.isEmpty()) {
            int current = queue.poll();
            result[index++] = current;

            for (int neighbor : adj.get(current)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (index != numCourses) {
            return new int[0];
        }
        return result;
    }

}
