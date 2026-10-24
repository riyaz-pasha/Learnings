/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * You are given a movie renting company with n shops. You need to implement 
 * a renting system supporting:
 * - Search: Find the 5 cheapest unrented copies of a given movie across all 
 *   shops. (Sorted by price asc, then shop asc).
 * - Rent: Rent an unrented copy of a movie from a specific shop.
 * - Drop: Return a rented movie to a specific shop.
 * - Report: Find the 5 cheapest rented movies across ALL shops and movies. 
 *   (Sorted by price asc, then shop asc, then movie asc).
 * 
 * Constraints:
 * - Up to 3 * 10^5 shops.
 * - Up to 10^5 initial entries.
 * - Up to 10^5 calls to search, rent, drop, and report.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are the prices for a specific (shop, movie) pair guaranteed to be constant?
 *    (Yes. A movie's price at a given shop never changes, which allows us to safely cache prices for O(1) lookups).
 * 2. Can two shops have the exact same price for the exact same movie?
 *    (Yes, which is why the sorting tie-breaker explicitly relies on the unique shop ID).
 * 3. Does `rent` or `drop` ever get called invalidly?
 *    (No, the prompt guarantees `rent` only applies to available movies, and `drop` to currently rented ones, saving us from checking existence).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need instantly sorted subsets of data (top 5 queries) 
 *   that can withstand continuous, high-frequency additions and removals. The 
 *   binding constraint is that arbitrary removal from standard priority queues 
 *   (Heaps) is painfully slow.
 * 
 * --- Approach 1: The Brute Force Lists ---
 * 1. Intuition: Keep an array or list of all unrented and rented movies. 
 * 2. Try: On `search` or `report`, filter the lists, sort them, and take the top 5.
 * 3. Why it works: Accurately reflects state.
 * 4. Why it's too costly: Sorting 10^5 elements per query across 10^5 queries takes 
 *    O(Q * N log N) time, which will instantly Time Out (TLE).
 * 5. Bottleneck: Re-sorting unchanged data repeatedly.
 * 
 * Time Complexity: rent/drop: O(1). search/report: O(N log N).
 * Space Complexity: O(N).
 * 
 * --- Approach 2: Priority Queues (Min-Heaps) ---
 * 1. Intuition: We only need the top 5, so a Min-Heap seems perfect!
 * 2. Try: Maintain a Min-Heap of rented movies, and a Map of Min-Heaps for unrented movies.
 * 3. Why it fails: While getting the top 5 is O(1), `rent` and `drop` require moving a 
 *    specific movie between heaps. `PriorityQueue.remove(Object)` in Java triggers a 
 *    linear scan to find the object, making it O(N).
 * 4. Bottleneck: Arbitrary O(N) deletion in heaps destroys performance.
 * 
 * --- Approach 3: Balanced Binary Search Trees (TreeSets) [Optimal] ---
 * 1. Intuition: We need a data structure that maintains sorted order AND allows O(log N) 
 *    arbitrary removals. A Red-Black Tree (Java's TreeSet) provides exactly this.
 * 2. Try: 
 *    - `Map<Integer, TreeSet<Entry>> unrented`: Maps a movie ID to a sorted set of available copies.
 *    - `TreeSet<Entry> rented`: A global sorted set of currently rented movies.
 *    - `Map<Long, Integer> prices`: A quick lookup map to get a price for any (shop, movie) in O(1).
 * 3. Why it works: 
 *    - `search` and `report` just grab the first 5 elements from the respective TreeSet using an iterator (O(1)).
 *    - `rent` and `drop` instantiate an `Entry` object and move it between sets in strict O(log N) time.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It showcases advanced system design awareness, knowing exactly when a PriorityQueue's 
 *    linear removal penalty forces the use of a Balanced BST.
 * 
 * Time Complexity: 
 * - Init: O(N log N) — inserting N entries.
 * - search / report: O(1) — reading the first 5 nodes of a TreeSet.
 * - rent / drop: O(log N) — O(1) price lookup, O(log N) TreeSet removal/addition.
 * Space Complexity: O(N) — storing at most N entries across the maps and sets.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Fewer than 5 results available: The iterator must gracefully stop when `hasNext()` is false.
 * - Extremely large shop/movie IDs: Hashing `(shop, movie)` as a string `shop + "-" + movie` generates 
 *   heavy object allocation overhead during 10^5 operations. Using bitwise operations to combine them 
 *   into a primitive `long` key avoids GC pressure entirely.
 * - Total consistency between `equals()` and `compareTo()`: For TreeSet to correctly remove an object, 
 *   `compareTo` must uniquely distinguish every element. If it returns 0, the set considers them identical. 
 *   We must chain `price -> shop -> movie` comparisons.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "TreeSets fix Heap deletion limits." 
 *   Whenever a problem asks for Top K queries BUT requires you to surgically remove specific 
 *   elements that aren't at the top, immediately abandon Heaps and use Balanced BSTs.
 * 
 * ASCII Architecture Diagram:
 * 
 * [ PRICES MAP ] (O(1) lookups)
 *   Key: (shop << 32 | movie) -> Value: price
 * 
 * [ UNRENTED MAP ] (Groups by Movie)
 *   Movie 1 -> TreeSet [ (price 4, shop 2), (price 5, shop 1) ] -> top 5 for search
 *   Movie 2 -> TreeSet [ (price 9, shop 0) ]
 * 
 * [ RENTED SET ] (Global)
 *   TreeSet [ (price 2, shop 1, mov 3), (price 5, shop 0, mov 1) ] -> top 5 for report
 * 
 * Pitfalls:
 * - Omitting properties in `compareTo`: If you sort by just `price` and `shop`, but omit `movie` 
 *   in the global `rented` set, two different rented movies from the same shop with the same price 
 *   will collide, and the TreeSet will silently refuse to add the second one!
 * 
 * Pattern Recognition: "Top K elements + Middle Deletions" -> TreeSet with custom Comparable.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `search` requested the top 50,000 instead of the top 5?
 * A1: Fetching 50,000 elements from a TreeSet via an iterator takes O(K) time, which would become 
 *     a bottleneck. We could mitigate this by maintaining a separate, eagerly updated array/list 
 *     of Top K elements, or implementing pagination so the client fetches them in chunks.
 * 
 * Q2: How would this behave in a heavily multi-threaded environment (e.g., real ticket booking)?
 * A2: TreeSets are not thread-safe. Wrapping them in `Collections.synchronizedSortedSet` works, but 
 *     creates global bottlenecks. In high concurrency, we would use `ConcurrentSkipListSet`, which 
 *     is a lock-free, thread-safe alternative offering the same O(log N) time bounds.
 */

import java.util.*;

public class MovieRentalSystemStudy {

    static class MovieRentingSystem {
        
        /**
         * Custom Entry class representing a specific movie copy.
         * Implements Comparable to define strict ordering for our TreeSets.
         */
        private static class Entry implements Comparable<Entry> {
            int price;
            int shop;
            int movie;

            public Entry(int price, int shop, int movie) {
                this.price = price;
                this.shop = shop;
                this.movie = movie;
            }

            @Override
            public int compareTo(Entry o) {
                // 1. Sort by price (ascending)
                if (this.price != o.price) {
                    return Integer.compare(this.price, o.price);
                }
                // 2. Tie-breaker: Sort by shop ID (ascending)
                if (this.shop != o.shop) {
                    return Integer.compare(this.shop, o.shop);
                }
                // 3. Final tie-breaker: Sort by movie ID (ascending). 
                // CRITICAL for the global rented set to prevent collisions!
                return Integer.compare(this.movie, o.movie);
            }
        }

        // Maps a movie ID to a sorted set of unrented copies (Entry objects)
        private final Map<Integer, TreeSet<Entry>> unrented;
        
        // Global sorted set containing all currently rented copies
        private final TreeSet<Entry> rented;
        
        // Quick lookup mapping a combined (shop, movie) key to its price
        // We use bitwise shifting to combine two 32-bit ints into a single 64-bit long.
        // This is much faster and more memory-efficient than allocating String keys like "shop_movie".
        private final Map<Long, Integer> prices;

        /**
         * Initializes the object with n shops and the movies in entries.
         * Time Complexity: O(E log E) where E is the length of entries.
         */
        public MovieRentingSystem(int n, int[][] entries) {
            unrented = new HashMap<>();
            rented = new TreeSet<>();
            prices = new HashMap<>();

            for (int[] entry : entries) {
                int shop = entry[0];
                int movie = entry[1];
                int price = entry[2];

                // Cache the price using the composite bitwise key
                long compositeKey = getCompositeKey(shop, movie);
                prices.put(compositeKey, price);

                // Add to the unrented pool for this specific movie
                unrented.computeIfAbsent(movie, k -> new TreeSet<>())
                        .add(new Entry(price, shop, movie));
            }
        }

        /**
         * Returns a list of the 5 cheapest shops that have an unrented copy of the given movie.
         * Time Complexity: O(1) - Iterating up to 5 elements takes constant time.
         */
        public List<Integer> search(int movie) {
            List<Integer> result = new ArrayList<>();
            TreeSet<Entry> available = unrented.get(movie);

            if (available == null || available.isEmpty()) {
                return result; // Empty list if no unrented copies exist
            }

            // Iterate over the sorted set and pick the top 5
            int count = 0;
            for (Entry e : available) {
                result.add(e.shop);
                count++;
                if (count == 5) {
                    break;
                }
            }

            return result;
        }

        /**
         * Rents the given movie from the given shop.
         * Time Complexity: O(log U + log R) -> O(log N).
         */
        public void rent(int shop, int movie) {
            // O(1) price lookup
            int price = prices.get(getCompositeKey(shop, movie));
            Entry entry = new Entry(price, shop, movie);

            // O(log U) removal from unrented
            unrented.get(movie).remove(entry);
            
            // O(log R) addition to rented
            rented.add(entry);
        }

        /**
         * Drops off a previously rented movie at the given shop.
         * Time Complexity: O(log U + log R) -> O(log N).
         */
        public void drop(int shop, int movie) {
            // O(1) price lookup
            int price = prices.get(getCompositeKey(shop, movie));
            Entry entry = new Entry(price, shop, movie);

            // O(log R) removal from rented
            rented.remove(entry);
            
            // O(log U) addition back to unrented
            unrented.get(movie).add(entry);
        }

        /**
         * Returns a list of the 5 cheapest rented movies across all shops.
         * Time Complexity: O(1) - Iterating up to 5 elements takes constant time.
         */
        public List<List<Integer>> report() {
            List<List<Integer>> result = new ArrayList<>();
            
            int count = 0;
            for (Entry e : rented) {
                result.add(Arrays.asList(e.shop, e.movie));
                count++;
                if (count == 5) {
                    break;
                }
            }
            
            return result;
        }

        /**
         * Helper method to generate a unique 64-bit composite key from two 32-bit ints.
         */
        private long getCompositeKey(int shop, int movie) {
            return (((long) shop) << 32) | (movie & 0xFFFFFFFFL);
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        int n = 3;
        int[][] entries = {
            {0, 1, 5}, {0, 2, 6}, {0, 3, 7},
            {1, 1, 4}, {1, 2, 7},
            {2, 1, 5}
        };

        MovieRentingSystem system = new MovieRentingSystem(n, entries);

        // 1. Search for movie 1
        // Available: (Shop 1, Price 4), (Shop 0, Price 5), (Shop 2, Price 5)
        System.out.println("Search 1: " + system.search(1)); 
        // Expected: [1, 0, 2] (Price asc, then Shop ID asc for the tie)

        // 2. Rent movies
        system.rent(0, 1);
        system.rent(1, 2);

        // 3. Report rented movies
        // Rented: (Shop 0, Movie 1, Price 5) and (Shop 1, Movie 2, Price 7)
        System.out.println("Report: " + system.report());
        // Expected: [[0, 1], [1, 2]]

        // 4. Drop a movie
        system.drop(1, 2);

        // 5. Search for movie 2
        // Available: (Shop 0, Price 6) and (Shop 1, Price 7)
        System.out.println("Search 2: " + system.search(2));
        // Expected: [0, 1]
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Map of TreeSets + Global TreeSet + Hash Map index.
 * Key Observation: When an element needs to be repeatedly moved between collections 
 * AND those collections need to instantly answer Top-K sorted queries, Balanced 
 * BSTs (TreeSets) completely outclass PriorityQueues (Heaps) because they guarantee 
 * O(log N) deletion for arbitrary elements.
 * What to Memorize: Bitwise composite keys `((long) a << 32) | b` drastically 
 * reduce garbage collection overhead compared to String concatenation keys in highly 
 * iterative environments.
 * Most Common Trap: Incomplete `compareTo` methods. If your `compareTo` evaluates 
 * to 0 for two distinct items (e.g. failing to include `movie` as the final tiebreaker 
 * in the global rented set), the TreeSet will silently drop the second item!
 * Mental Trigger: "Top K queries + surgical removal" -> TreeSet Custom Objects.
 * ============================================================================
 */



























































import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

class MovieRentingSystem {

    /*
     * A shop/movie pair uniquely identifies a physical movie copy.
     *
     * Why use a record?
     * - It gives us immutable fields, equals(), and hashCode().
     * - HashMap can use it as a reliable composite key.
     * - We avoid error-prone manual key encoding, such as shop * 100_000 + movie.
     */
    private record CopyKey(int shop, int movie) {}

    /*
     * A movie copy's shop, movie ID, and price never change.
     *
     * We do not store a mutable "rented" flag here. Rental state is represented
     * by membership in the appropriate TreeSet, avoiding duplicated state
     * that could become inconsistent with the indexes.
     */
    private record Copy(int shop, int movie, int price) {}

    /*
     * Direct lookup:
     *
     * (shop, movie) -> Copy, including its fixed price.
     *
     * This lets rent() and drop() locate a copy in expected O(1) time.
     * It also avoids scanning entries to find a copy's price.
     */
    private final Map<CopyKey, Copy> copiesByKey = new HashMap<>();

    /*
     * Available index:
     *
     * movie ID -> all currently available copies of that movie.
     *
     * Each TreeSet is ordered by price, then shop.
     * Therefore, its iterator begins with the cheapest available copy.
     *
     * The map is sparse: we only need sets for movie IDs that have entries.
     */
    private final Map<Integer, TreeSet<Copy>> availableByMovie =
            new HashMap<>();

    /*
     * Rented index:
     *
     * One global TreeSet containing all rented copies.
     *
     * The ordering is price, then shop, then movie.
     * This is exactly the ordering required by report().
     */
    private final TreeSet<Copy> rentedCopies =
            new TreeSet<>(
                    Comparator.comparingInt(Copy::price)
                            .thenComparingInt(Copy::shop)
                            .thenComparingInt(Copy::movie)
            );

    /*
     * For a particular movie, shop ID resolves equal-price ties.
     *
     * Movie ID is not needed in this comparator because each set in
     * availableByMovie contains copies of exactly one movie.
     *
     * Since a shop carries at most one copy of a given movie, no two
     * distinct copies in this set can share both price and shop.
     */
    private static final Comparator<Copy> AVAILABLE_ORDER =
            Comparator.comparingInt(Copy::price)
                    .thenComparingInt(Copy::shop);

    public MovieRentingSystem(int n, int[][] entries) {

        /*
         * n is not required for the implementation: entries already specify
         * every available shop/movie copy.
         *
         * Build all indexes once. Initially, every copy is available and
         * no copy is rented.
         *
         * For M entries:
         * - HashMap insertion takes expected O(1) per entry.
         * - TreeSet insertion takes O(log M) per entry.
         * Total initialization: O(M log M).
         */
        for (int[] entry : entries) {
            int shop = entry[0];
            int movie = entry[1];
            int price = entry[2];

            Copy copy = new Copy(shop, movie, price);

            copiesByKey.put(new CopyKey(shop, movie), copy);

            availableByMovie
                    .computeIfAbsent(movie, ignored -> new TreeSet<>(AVAILABLE_ORDER))
                    .add(copy);
        }
    }

    public List<Integer> search(int movie) {

        /*
         * Retrieve the pre-sorted set for this movie.
         *
         * If the movie does not exist, there are no matching copies.
         * HashMap lookup takes expected O(1) time.
         */
        TreeSet<Copy> available = availableByMovie.get(movie);

        if (available == null || available.isEmpty()) {
            return List.of();
        }

        List<Integer> result = new ArrayList<>(5);

        /*
         * TreeSet iteration visits copies in price/shop order.
         * We only visit at most five elements, so we do not sort or scan
         * all copies belonging to the movie.
         *
         * Time: O(1 + min(5, K)) = O(1), where K is the number of
         * available copies of this movie. The constant 5 is fixed.
         *
         * Space: O(1) auxiliary space for at most five result elements.
         */
        for (Copy copy : available) {
            result.add(copy.shop());

            if (result.size() == 5) {
                break;
            }
        }

        return result;
    }

    public void rent(int shop, int movie) {

        /*
         * Find the exact copy in expected O(1) time.
         * The problem guarantees that this copy is currently available.
         */
        Copy copy = copiesByKey.get(new CopyKey(shop, movie));

        /*
         * Remove it from the movie's available set.
         *
         * TreeSet removal costs O(log K), where K is the size of that
         * movie's available set. Because Copy is immutable and its
         * comparator fields do not change, removal uses the same ordering
         * under which the copy was inserted.
         */
        TreeSet<Copy> available = availableByMovie.get(movie);
        available.remove(copy);

        /*
         * Add the same copy to the global rented set.
         *
         * TreeSet insertion costs O(log R), where R is the number of
         * rented copies after the transition.
         *
         * Both indexes are updated as part of the same operation:
         * the copy is no longer available and is now rented.
         */
        rentedCopies.add(copy);
    }

    public void drop(int shop, int movie) {

        /*
         * The problem guarantees that this copy is currently rented.
         * The price is recovered from the canonical copy record.
         */
        Copy copy = copiesByKey.get(new CopyKey(shop, movie));

        /*
         * Remove from the global rented index in O(log R).
         * The record's price, shop, and movie are unchanged, so the
         * comparator can locate the same element correctly.
         */
        rentedCopies.remove(copy);

        /*
         * Restore the copy to the available index for its movie.
         *
         * Insertion costs O(log K), where K is the resulting set size.
         * We reuse the existing set even if it temporarily became empty.
         */
        availableByMovie.get(movie).add(copy);
    }

    public List<List<Integer>> report() {

        if (rentedCopies.isEmpty()) {
            return List.of();
        }

        List<List<Integer>> result = new ArrayList<>(5);

        /*
         * The global TreeSet already enforces:
         *
         *   price ascending
         *   -> shop ascending
         *   -> movie ascending
         *
         * Therefore, no additional sorting is needed.
         *
         * We stop after five copies, so we never traverse all R rented
         * copies unless R < 5.
         *
         * Time: O(1) for the fixed five-element traversal.
         * Space: O(1) auxiliary space for at most five result pairs.
         */
        for (Copy copy : rentedCopies) {
            result.add(List.of(copy.shop(), copy.movie()));

            if (result.size() == 5) {
                break;
            }
        }

        return result;
    }
}



