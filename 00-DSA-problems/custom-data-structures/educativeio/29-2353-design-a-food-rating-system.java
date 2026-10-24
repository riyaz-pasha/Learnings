/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a food rating system that can do the following:
 * - Modify the rating of a food item listed in the system.
 * - Return the highest-rated food item for a type of cuisine in the system.
 * 
 * Implement the FoodRatings class:
 * - FoodRatings(foods, cuisines, ratings): Initializes the system.
 * - void changeRating(food, newRating): Changes the rating of the given food.
 * - String highestRated(cuisine): Returns the name of the highest-rated food 
 *   for the given cuisine. Tie-breaker: lexicographically smaller name.
 * 
 * Constraints: 1 <= n <= 2 * 10^4. 1 <= ratings[i] <= 10^8.
 * At most 2 * 10^4 calls will be made in total to changeRating and highestRated.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Does a food item uniquely map to exactly one cuisine?
 *    (Yes, the input arrays define a strict 1:1 mapping between a food and its cuisine).
 * 2. Can the same food item appear multiple times in the initialization arrays?
 *    (Constraints say "All the strings in foods are distinct", saving us from handling duplicate init logic).
 * 3. Does `highestRated` just need the name, or the rating too?
 *    (Just the name, simplifying the return type).
 * 4. Can the rating become negative?
 *    (Constraints say 1 <= ratings <= 10^8, so it stays positive).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need O(1) lookup to find what cuisine a food belongs to, 
 *   and fast access to the maximum rating per cuisine. The binding constraint is 
 *   that changing a rating must dynamically re-sort or re-evaluate the maximums 
 *   efficiently across 20,000 operations.
 * 
 * --- Approach 1: HashMap + Brute Force ---
 * 1. Intuition: Map each cuisine to a list of its foods. 
 * 2. Try: `HashMap<String, List<Food>>`. `changeRating` finds the food in O(1) with 
 *    a secondary map and updates its score. `highestRated` iterates through the entire 
 *    cuisine list to find the max.
 * 3. Why it fails: `highestRated` takes O(N) time. With 20,000 calls, scanning long 
 *    cuisine lists repeatedly will cause a Time Limit Exceeded (TLE).
 * 
 * --- Approach 2: Priority Queue (Max-Heap) with Lazy Deletion ---
 * 1. Intuition: A Max-Heap per cuisine keeps the highest-rated food at the top in O(1).
 * 2. Try: `HashMap<String, PriorityQueue<Food>>`. 
 *    `changeRating`: Push a *new* instance of the food `(name, newRating)` into the heap. 
 *    Update the "true" rating in a master `HashMap<String, Integer>`.
 *    `highestRated`: Peek at the top of the heap. If the heap node's rating doesn't match 
 *    the master map's true rating, pop it (lazy deletion) and check the next one.
 * 3. Why it works: We avoid O(N) heap removals. Pushing takes O(log K). 
 * 4. Trade-off: Memory footprint grows with every `changeRating` call because we leave "ghost" 
 *    nodes in the heap.
 * 
 * --- Approach 3: Balanced BST (TreeSet) [Optimal & Strict] ---
 * 1. Intuition: We need surgical O(log N) removals and insertions without memory bloat.
 * 2. Try: `HashMap<String, TreeSet<Food>>`. 
 * 3. Why it works: `TreeSet` keeps elements perfectly sorted according to a custom `compareTo` 
 *    method (rating descending, name ascending). 
 *    `changeRating`: Remove the food from the set (O(log N)), mutate its rating, and re-add it (O(log N)).
 *    `highestRated`: `set.first().name` (O(1)).
 * 4. Which to write in a 45-min interview?
 *    Approach 3. While lazy-deletion heaps are great, TreeSets demonstrate strict memory 
 *    management and strong OOP skills by overriding `Comparable`.
 * 
 * Time Complexity: 
 * - changeRating: O(log N) — removing and re-adding to the TreeSet.
 * - highestRated: O(1) — reading the first node of the TreeSet.
 * Space Complexity: O(N) — strictly bounded memory, no ghost nodes.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Lexicographical ties: "apple" vs "banana". `apple` comes first. `String.compareTo()` 
 *   handles this natively. Note: Ascending lexicographical order means smaller strings 
 *   are "greater" in our priority ranking.
 * - Modifying an object while it is inside a TreeSet: This completely corrupts the tree's 
 *   internal structure. You **must** remove the object, modify it, and then re-insert it.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "TreeSets require flawless CompareTo alignment." 
 *   If your `compareTo` method says two objects are "equal" (returns 0), the TreeSet 
 *   assumes they are the exact same object and won't add the second one. Since names 
 *   are unique, using `rating` then `name` ensures every food evaluates uniquely.
 * 
 * ASCII DRY RUN (changeRating operation)
 * 1. Initial State:
 *    FoodMap: { "sushi" -> (rating:8, cuisine:"japanese") }
 *    TreeSet("japanese"): [ (ramen, 14), (sushi, 8) ]
 * 
 * 2. changeRating("sushi", 16)
 *    Step A: Get object from FoodMap.
 *    Step B: REMOVE object from TreeSet. 
 *            TreeSet("japanese"): [ (ramen, 14) ]
 *    Step C: Update rating to 16.
 *    Step D: RE-ADD object to TreeSet.
 *            TreeSet("japanese"): [ (sushi, 16), (ramen, 14) ]  <-- sushi is now first!
 * 
 * Pitfalls:
 * - Forgetting to remove the object *before* changing its rating. If you change the rating 
 *   first, the TreeSet's hash/tree path is broken, and `remove(food)` will silently fail, 
 *   leaving a permanently orphaned, un-deletable node in your tree.
 * 
 * Pattern Recognition: "Top item per category + dynamic updates" -> Map of TreeSets.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `changeRating` was called 10 million times?
 * A1: The TreeSet approach does log(N) operations per call. Java's object creation and tree 
 *     rebalancing might incur heavy GC overhead. In a massive scale system, the Lazy-Deletion 
 *     Heap approach might actually be faster for writes (because Heap insertion is faster than 
 *     Red-Black tree rebalancing), provided we have a background thread to occasionally prune 
 *     the heap of ghost nodes to prevent memory exhaustion.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class FoodRatingStudy {

    /**
     * Represents a single food item. 
     * Implements Comparable to define strict sorting logic for the TreeSet.
     */
    static class Food implements Comparable<Food> {
        String name;
        String cuisine;
        int rating;

        public Food(String name, String cuisine, int rating) {
            this.name = name;
            this.cuisine = cuisine;
            this.rating = rating;
        }

        @Override
        public int compareTo(Food other) {
            // Primary sort: Rating descending (highest rating first)
            if (this.rating != other.rating) {
                return Integer.compare(other.rating, this.rating);
            }
            // Secondary sort: Lexicographical order ascending (smaller string first)
            return this.name.compareTo(other.name);
        }
    }

    static class FoodRatings {
        
        // Quick O(1) lookup to find the Food object by its name
        private final Map<String, Food> foodMap;
        
        // Maps a cuisine to a sorted set of its Food objects
        private final Map<String, TreeSet<Food>> cuisineMap;

        /**
         * Initializes the system.
         */
        public FoodRatings(String[] foods, String[] cuisines, int[] ratings) {
            foodMap = new HashMap<>();
            cuisineMap = new HashMap<>();

            for (int i = 0; i < foods.length; i++) {
                Food foodItem = new Food(foods[i], cuisines[i], ratings[i]);
                
                foodMap.put(foods[i], foodItem);
                
                // Initialize the TreeSet for this cuisine if it doesn't exist yet
                cuisineMap.computeIfAbsent(cuisines[i], k -> new TreeSet<>()).add(foodItem);
            }
        }

        /**
         * Changes the rating of the food item with the name food.
         * Time Complexity: O(log N)
         */
        public void changeRating(String food, int newRating) {
            // O(1) lookup to get the food object
            Food foodItem = foodMap.get(food);
            
            // Get the corresponding cuisine's sorted set
            TreeSet<Food> sortedFoods = cuisineMap.get(foodItem.cuisine);

            // CRITICAL: We MUST remove the item from the TreeSet BEFORE modifying its rating.
            // If we modify it first, the tree's structure breaks and the removal fails.
            sortedFoods.remove(foodItem);

            // Update the rating
            foodItem.rating = newRating;

            // Re-insert it into the TreeSet so it assumes its new correct sorted position
            sortedFoods.add(foodItem);
        }

        /**
         * Returns the name of the food item that has the highest rating for the given cuisine.
         * Time Complexity: O(1)
         */
        public String highestRated(String cuisine) {
            // first() instantly grabs the highest-priority element as defined by our compareTo
            return cuisineMap.get(cuisine).first().name;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        String[] foods = {"kimchi", "miso", "sushi", "moussaka", "ramen", "bulgogi"};
        String[] cuisines = {"korean", "japanese", "japanese", "greek", "japanese", "korean"};
        int[] ratings = {9, 12, 8, 15, 14, 7};

        FoodRatings foodRatings = new FoodRatings(foods, cuisines, ratings);

        // Highest rated korean is kimchi (9)
        System.out.println("Highest rated korean: " + foodRatings.highestRated("korean")); 
        
        // Highest rated japanese is ramen (14)
        System.out.println("Highest rated japanese: " + foodRatings.highestRated("japanese")); 
        
        // Update sushi from 8 to 16
        System.out.println("Changing sushi rating to 16...");
        foodRatings.changeRating("sushi", 16);
        
        // Highest rated japanese is now sushi (16)
        System.out.println("Highest rated japanese: " + foodRatings.highestRated("japanese")); 
        
        // Update ramen from 14 to 16
        System.out.println("Changing ramen rating to 16...");
        foodRatings.changeRating("ramen", 16);
        
        // Both sushi and ramen have a rating of 16.
        // "ramen" is lexicographically smaller than "sushi", so ramen wins the tie.
        System.out.println("Highest rated japanese: " + foodRatings.highestRated("japanese")); // Expected: ramen
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: HashMap (O(1) lookups) + HashMap of TreeSets (O(1) max queries).
 * Key Observation: Updating a dynamically sorted property requires surgical removal 
 * and reinsertion. A Balanced BST (TreeSet) guarantees O(log N) arbitrary deletion, 
 * whereas a PriorityQueue (Heap) requires O(N) deletion unless using a lazy-deletion strategy.
 * What to Memorize: `set.remove(obj); obj.val = newVal; set.add(obj);`. The order is paramount.
 * Most Common Trap: Modifying the `rating` of the `Food` object BEFORE calling `remove()`. 
 * Because `TreeSet` relies on `compareTo` to find the node, changing the rating alters its 
 * comparative path, causing `remove()` to silently fail and duplicate the object in the tree.
 * Mental Trigger: "Change specific value + Find Max" -> "Remove, Modify, Re-add in TreeSet".
 * ============================================================================
 */













































import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

class FoodRatings {

    /*
     * Immutable name and cuisine, mutable rating.
     *
     * Cuisine is fixed by the problem. Keeping it on the food object means
     * changeRating can find the correct cuisine set without another lookup.
     *
     * A record would be a poor fit for the mutable rating itself because
     * records are designed for immutable data carriers.
     */
    private static final class Food {
        private final String name;
        private final String cuisine;
        private int rating;

        private Food(String name, String cuisine, int rating) {
            this.name = name;
            this.cuisine = cuisine;
            this.rating = rating;
        }
    }

    /*
     * Index 1: food name -> Food object.
     *
     * This gives expected O(1) lookup when changeRating receives a food name.
     * It also provides the authoritative current rating.
     */
    private final Map<String, Food> foodsByName = new HashMap<>();

    /*
     * Index 2: cuisine -> foods ranked from best to worst.
     *
     * Each cuisine has its own TreeSet, so querying Japanese food does not
     * require inspecting Korean, Greek, or any other cuisine.
     */
    private final Map<String, NavigableSet<Food>> foodsByCuisine =
            new HashMap<>();

    /*
     * Ranking rules:
     *
     * 1. Higher rating comes first, so compare ratings in descending order.
     * 2. Equal ratings are ordered by food name in ascending lexicographic
     *    order.
     * 3. Names are unique, so distinct foods cannot compare as equal.
     *
     * Integer.compare avoids subtraction-based comparator overflow.
     * String.compareTo implements the required lexicographic ordering.
     */
    private static final Comparator<Food> FOOD_RANKING =
            Comparator.comparingInt((Food food) -> food.rating)
                    .reversed()
                    .thenComparing(food -> food.name);

    public FoodRatings(
            String[] foods,
            String[] cuisines,
            int[] ratings) {

        for (int i = 0; i < foods.length; i++) {
            Food food = new Food(foods[i], cuisines[i], ratings[i]);

            // Register the food for direct name-based lookup.
            foodsByName.put(food.name, food);

            // Create the cuisine's ordered set the first time we encounter it.
            foodsByCuisine
                    .computeIfAbsent(
                            food.cuisine,
                            ignored -> new TreeSet<>(FOOD_RANKING))
                    .add(food);
        }
    }

    public void changeRating(String foodName, int newRating) {
        // Step 1: Find the existing object in expected O(1) time.
        Food food = foodsByName.get(foodName);

        // Step 2: Find the ordered set for this food's fixed cuisine.
        NavigableSet<Food> cuisineFoods =
                foodsByCuisine.get(food.cuisine);

        /*
         * Step 3: Remove the food BEFORE changing its rating.
         *
         * The TreeSet orders elements using rating and name. Changing rating
         * while the object remains in the set would invalidate the ordering
         * assumptions used by the tree.
         *
         * Removal is O(log k), where k is the number of foods in this cuisine.
         * It works because the comparator still sees the old rating.
         */
        cuisineFoods.remove(food);

        // Step 4: Update the authoritative Food object.
        food.rating = newRating;

        /*
         * Step 5: Reinsert using the new rating.
         *
         * TreeSet finds the correct position under the updated comparator
         * values. After insertion, the set is correctly ordered again.
         */
        cuisineFoods.add(food);
    }

    public String highestRated(String cuisine) {
        /*
         * first() returns the smallest element according to FOOD_RANKING.
         * Since that comparator puts the best food first, no scan is needed.
         *
         * TreeSet.first() is O(1) for a non-empty set.
         * The problem guarantees that the requested cuisine exists.
         */
        return foodsByCuisine.get(cuisine).first().name;
    }

    /*
     * COMPLEXITY ANALYSIS
     *
     * Let:
     * n = total number of foods.
     * k = number of foods in the cuisine being updated.
     *
     * Constructor:
     * - Each food enters a HashMap in expected O(1) time.
     * - Each food enters a TreeSet in O(log k) time for its cuisine.
     * - Total: O(n log n) worst-case upper bound, with O(n) storage.
     *
     * changeRating:
     * - HashMap lookup: O(1) expected.
     * - TreeSet removal: O(log k).
     * - Rating assignment: O(1).
     * - TreeSet insertion: O(log k).
     * - Total: O(log k), bounded by O(log n).
     *
     * highestRated:
     * - HashMap lookup: O(1) expected.
     * - TreeSet.first(): O(1).
     * - Total: O(1) expected.
     *
     * Space:
     * - O(n) Food objects in the name map.
     * - O(n) references across all cuisine-specific TreeSets.
     * - Total: O(n).
     *
     * Why this is optimal for the intended design:
     * We avoid scanning all foods for every query, while maintaining the
     * ranking incrementally after each update. TreeSet updates cost
     * logarithmic time and the highest-ranked food is immediately accessible.
     */
}










































import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class FoodRatingsWithLazyHeap {

    private record FoodState(String name, String cuisine, int rating) {}

    // The current, authoritative rating for every food.
    private final Map<String, FoodState> currentFoods = new HashMap<>();

    // Each cuisine has a heap ordered by best rating, then smallest name.
    private final Map<String, PriorityQueue<FoodState>> heapsByCuisine =
            new HashMap<>();

    private static final Comparator<FoodState> BEST_FIRST =
            Comparator.comparingInt(FoodState::rating)
                    .reversed()
                    .thenComparing(FoodState::name);

    public FoodRatingsWithLazyHeap(
            String[] foods, String[] cuisines, int[] ratings) {

        for (int i = 0; i < foods.length; i++) {
            FoodState state =
                    new FoodState(foods[i], cuisines[i], ratings[i]);

            currentFoods.put(state.name(), state);

            heapsByCuisine
                    .computeIfAbsent(
                            state.cuisine(),
                            ignored -> new PriorityQueue<>(BEST_FIRST))
                    .offer(state);
        }
    }

    public void changeRating(String foodName, int newRating) {
        FoodState oldState = currentFoods.get(foodName);

        // Replace the authoritative state with a new immutable record.
        FoodState newState =
                new FoodState(oldState.name(), oldState.cuisine(), newRating);

        currentFoods.put(foodName, newState);

        /*
         * Do not attempt to remove oldState from the heap.
         * PriorityQueue.remove(Object) is O(k), not O(log k).
         * Add the new state instead; the old entry becomes stale.
         */
        heapsByCuisine.get(newState.cuisine()).offer(newState);
    }

    public String highestRated(String cuisine) {
        PriorityQueue<FoodState> heap = heapsByCuisine.get(cuisine);

        /*
         * A food can have multiple entries in the heap after multiple
         * rating changes. Only the entry matching the current authoritative
         * rating is valid.
         *
         * Because food names are unique, comparing the rating is sufficient
         * to identify a stale entry: all entries for that food have its name.
         */
        while (!heap.isEmpty()) {
            FoodState candidate = heap.peek();
            FoodState current = currentFoods.get(candidate.name());

            if (candidate.rating() == current.rating()) {
                return candidate.name();
            }

            // Discard a stale entry. It can never become current again.
            heap.poll();
        }

        throw new IllegalStateException("Cuisine has no valid food");
    }

    /*
     * Let n be the initial number of foods, u the number of rating updates,
     * and k the size of a cuisine's heap, including stale entries.
     *
     * Constructor: O(n log n) worst-case upper bound.
     * changeRating: O(log k) for insertion.
     * highestRated: O(1) if the heap top is valid; otherwise O(s log k)
     * for s stale entries discarded during that call.
     *
     * Across the entire execution, every inserted entry is polled at most
     * once. Therefore, all stale-entry cleanup costs O((n + u) log(n + u))
     * in aggregate. Individual queries can nevertheless be expensive.
     *
     * Space: O(n + u), because stale entries accumulate until removed.
     *
     * Interview trade-off:
     * The heap avoids arbitrary deletion but requires lazy cleanup and can
     * use more memory. TreeSet is generally the cleaner choice here because
     * rating changes can be handled with explicit removal and reinsertion.
     */
}

