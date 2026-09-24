import java.util.ArrayList;
import java.util.List;

class CourseSchedule {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        // build adjacency list
        List<List<Integer>> adj= this.buildAdj(numCourses, prerequisites);
        // directed graph cycle detection

        boolean[] visited=new boolean[numCourses];
        boolean[] pathVisited=new boolean[numCourses];
        for(int course=0;course<numCourses;course++){
            if(!visited[course]){
                if(!canFinish(adj,visited,pathVisited,course)){
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canFinish(List<List<Integer>> adj,boolean[] visited,boolean[] pathVisited,int course){
        visited[course]=true;
        pathVisited[course]=true;

        for(int neighbor:adj.get(course)){
            if(!visited[neighbor]){
                if(!canFinish(adj,visited,pathVisited,neighbor)){
                    return false;
                }
            }
            else if(pathVisited[neighbor]){
                return false;
            }
        }

        pathVisited[course]=false;
        return true;
    }


    private List<List<Integer>> buildAdj(int numCourses,int[][] prerequisites) {
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            adjList.add(new ArrayList<>());
        }

        for (int[] prerequisite : prerequisites) {
            adjList.get(prerequisite[1]).add(prerequisite[0]); // directed edge: prerequisite[1] -> prerequisite[0]
        }
        return adjList;
    }

}
