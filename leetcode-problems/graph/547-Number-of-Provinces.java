import java.util.Queue;
import java.util.LinkedList;

class NumberOfProvinces {
    public static void main(String[] args) {
        var solution = new ProvinceBFS();
        System.out.println(solution.noOfProvinces(new int[][]{{1, 1, 0}, {1, 1, 0}, {0, 0, 1}}));
        System.out.println(solution.noOfProvinces(new int[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}));
        System.out.println(solution.noOfProvinces(new int[][]{{1, 0, 0, 1}, {0, 1, 1, 0}, {0, 1, 1, 1}, {1, 0, 1, 1}}));
    }
}

class ProvincesDFS {
    public int noOfProvinces(int[][] isConnected) {
        int len = isConnected.length;
        boolean[] isCityVisited = new boolean[len];
        int province = 0;

        for (int city = 0; city < len; city++) {
            if (!isCityVisited[city]) {
                findConntectedCitiesDFS(isConnected, city, isCityVisited);
                province++;
            }
        }
        return province;
    }

    private void findConntectedCitiesDFS(int[][] isConntected, int city, boolean[] isCityVisited) {
        int len = isConntected.length;
        isCityVisited[city] = true;
        for (int neighbor = 0; neighbor < len; neighbor++) {
            if (isConntected[city][neighbor] == 1 && !isCityVisited[neighbor]) {
                findConntectedCitiesDFS(isConntected, neighbor, isCityVisited);
            }
        }
    }
}

class ProvinceBFS {
    public int noOfProvinces(int[][] isConnected) {
        int len = isConnected.length;
        boolean[] isCityVisited = new boolean[len];
        int province = 0;

        for (int city = 0; city < len; city++) {
            if (!isCityVisited[city]) {
                findConntectedCitiesBFS(isConnected, city, isCityVisited);
                province++;
            }
        }
        return province;
    }

    private void findConntectedCitiesBFS(int[][] isConnected, int startCity, boolean[] isCityVisited) {
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(startCity);
        isCityVisited[startCity] = true;

        while (!queue.isEmpty()) {
            int currentCity = queue.poll();
            for (int neighbor = 0; neighbor < isConnected.length; neighbor++) {
                if (isConnected[currentCity][neighbor] == 1 && !isCityVisited[neighbor]) {
                    queue.offer(neighbor);
                    isCityVisited[neighbor] = true;
                }
            }
        }
    }
}