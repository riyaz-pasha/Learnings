import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Record to keep track of traversal state for K-hops.
 */
record TraversalState(Movie movie, int depth) {}

class MovieRecommender {

    /**
     * Returns top N similar movies using a K-hop BFS approach.
     * * @param rootMovie The starting movie
     * @param n         Number of results to return
     * @param k         Maximum hops from the root movie
     * @return          List of movie names sorted by similarity score (desc)
     */
    public List<String> getTopNSimilarMovies(Movie rootMovie, int n, int k) {
        // 1. Validation
        if (rootMovie == null || n <= 0 || k < 0) {
            return Collections.emptyList();
        }

        // 2. Min-Heap to track top N: Smallest score stays at the top for easy removal
        PriorityQueue<SimilarMovie> topNHeap = new PriorityQueue<>(
            Comparator.comparingDouble(sm -> sm.score)
        );

        // 3. BFS Structures
        Queue<TraversalState> queue = new LinkedList<>();
        Set<Movie> visited = new HashSet<>();

        queue.offer(new TraversalState(rootMovie, 0));
        visited.add(rootMovie);

        while (!queue.isEmpty()) {
            TraversalState current = queue.poll();
            
            // Stop exploring neighbors if we've reached the hop limit
            if (current.depth() >= k) continue;

            List<SimilarMovie> neighbors = current.movie().similarMovies;
            if (neighbors == null) continue;

            for (SimilarMovie neighbor : neighbors) {
                if (neighbor.movie == null || visited.contains(neighbor.movie)) {
                    continue;
                }

                visited.add(neighbor.movie);
                
                // Add to heap logic: O(log N)
                updateHeap(topNHeap, neighbor, n);

                // Add to queue for next level: O(1)
                queue.offer(new TraversalState(neighbor.movie, current.depth() + 1));
            }
        }

        // 4. Result Construction
        return buildResultList(topNHeap);
    }

    private void updateHeap(PriorityQueue<SimilarMovie> heap, SimilarMovie candidate, int n) {
        if (heap.size() < n) {
            heap.offer(candidate);
        } else if (candidate.score > heap.peek().score) {
            heap.poll();
            heap.offer(candidate);
        }
    }

    private List<String> buildResultList(PriorityQueue<SimilarMovie> heap) {
        LinkedList<String> result = new LinkedList<>();
        while (!heap.isEmpty()) {
            // Using addFirst to avoid the need for Collections.reverse()
            result.addFirst(heap.poll().movie.name);
        }
        return result;
    }
}

