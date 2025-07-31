/* Given a movie name, return the top `n` similar movies. */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

class Movie {

    String name;
    List<SimilarMovie> similarMovies;

}

class SimilarMovie {

    Movie movie;
    Double score;

}

class Pair {

    Movie movie;
    int level;

    public Pair(Movie movie, int level) {
        this.movie = movie;
        this.level = level;
    }

}

class MovieRecommender {

    public List<String> getTopNSimilarMovies(Movie movie, int n) {
        // Top N (stores in reverse order)
        PriorityQueue<SimilarMovie> minHeap = new PriorityQueue<>(Comparator.comparingDouble(sm -> sm.score));

        // BFS
        Queue<Movie> queue = new LinkedList<>();
        Set<Movie> visited = new HashSet<>();

        queue.offer(movie); // O(1)
        visited.add(movie); // O(1)

        while (!queue.isEmpty()) {
            Movie current = queue.poll();
            for (SimilarMovie neighbor : current.similarMovies) {
                if (visited.contains(neighbor.movie)) {
                    continue;
                }
                queue.offer(neighbor.movie);
                visited.add(neighbor.movie);
                if (minHeap.size() > n) {
                    minHeap.poll(); // removes movie with lowest similarity score // O(log N)
                }
                minHeap.offer(neighbor); // O(log N)
            }
        }

        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) { // O(N (log N))
            result.add(minHeap.poll().movie.name); // O(log N)
        }
        // Collections.sort(result, Collections.reverseOrder());
        Collections.reverse(result); // O(N)
        return result;
    }
    /*
     * ✅ Time Complexity
     * V = number of movies (nodes)
     * E = number of similarMovie edges
     * N = number of top movies you want to return
     * 
     * Each movie is visited once → O(V)
     * Each similar movie edge is processed once → O(E)
     * ✅ So: BFS traversal = O(V + E)
     * 
     * You insert up to V-1 similar movies into the heap.
     * Each insertion/removal is O(log N)
     * But you only maintain top N, so max heap size = N.
     * ✅ So: Heap operations = O(V × log N) (in worst case)
     * 
     * ✅ Result construction = O(N log N)
     * 
     * ✅ Total Time Complexity:
     * O(V + E + V log N + N log N) ≈ O(V log N + E)
     * 
     * 
     * ✅ Space Complexity
     * 1. Visited Set
     * Stores up to V movies → O(V)
     * 
     * 2. Queue
     * In worst case, holds up to V movies → O(V)
     * 
     * 3. Min Heap
     * Holds only N movies → O(N)
     * 
     * 4. Result List
     * Final answer: N strings → O(N)
     * 
     * | Aspect | Complexity |
     * | --------- | -------------- |
     * | **Time** | O(V log N + E) |
     * | **Space** | O(V + N) |
     * 
     */

    public List<String> getTopNSimilarMoviesKHops(Movie movie, int n, int k) {
        Set<Movie> visited = new HashSet<>();
        Queue<Pair> queue = new LinkedList<>();
        PriorityQueue<SimilarMovie> minHeap = new PriorityQueue<>(Comparator.comparingDouble(sm -> sm.score));

        queue.offer(new Pair(movie, 0));
        visited.add(movie);

        while (!queue.isEmpty()) {
            Pair current = queue.poll();
            Movie currentMovie = current.movie;
            int level = current.level;

            if (level >= k)
                continue;

            for (SimilarMovie neighbor : currentMovie.similarMovies) {
                if (visited.contains(neighbor.movie))
                    continue;
                visited.add(neighbor.movie);
                queue.offer(new Pair(neighbor.movie, level + 1));

                if (minHeap.size() < n) {
                    minHeap.offer(neighbor);
                } else if (neighbor.score > minHeap.peek().score) {
                    minHeap.poll();
                    minHeap.offer(neighbor);
                }
            }
        }

        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll().movie.name);
        }
        Collections.reverse(result);
        return result;
    }

}
