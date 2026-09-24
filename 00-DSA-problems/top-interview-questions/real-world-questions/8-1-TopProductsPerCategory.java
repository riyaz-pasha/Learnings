/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Given a stream of product sales events, return the TOP N products PER CATEGORY
based on total sales.

Each event looks like:
(productId, category, quantitySold)

We want:
- Real-time updates
- Efficient retrieval of top N products per category
- Correct ranking by total sales

================================================================================
INTERVIEW FLOW (WHAT I WOULD SAY OUT LOUD)
--------------------------------------------------------------------------------
Step 1: Clarify requirements
- Each product belongs to exactly one category
- Sales come as a stream (incremental updates)
- We need top N PER CATEGORY (not global)
- Ranking is by total sales count
- Results should be available at any time

Step 2: Key observation
- This is NOT a single top-N problem
- It is a "top-N per group (category)" problem
- Best approach: isolate each category and solve top-N inside it

Step 3: Data structure choice
For each category:
- HashMap to track total sales per product
- Min-Heap of size N to track top N products

Globally:
- HashMap<Category, CategoryTracker>

Step 4: Complexity target
- Each sale update: O(log N)
- Query top N: O(N log N)
- Space proportional to number of products

================================================================================
*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/* =============================================================================
   PRODUCT ENTRY
   -----------------------------------------------------------------------------
   Represents a product and its total sales count.
   Stored inside the heap for ranking.
============================================================================= */
class ProductEntry {
    String productId;
    int totalSales;

    ProductEntry(String productId, int totalSales) {
        this.productId = productId;
        this.totalSales = totalSales;
    }
}

/* =============================================================================
   CATEGORY TRACKER
   -----------------------------------------------------------------------------
   Manages top N products for ONE category.

   Data Structures:
   1) Map<String, Integer> → productId → total sales
   2) Min-Heap (PriorityQueue) of size N → keeps top N products only

   Why Min-Heap?
   - Root always has the LOWEST sales among top N
   - Easy to evict weakest product when a better one appears
============================================================================= */
class CategoryTracker {

    private final int N;

    // Tracks total sales per product
    private final Map<String, Integer> salesMap = new HashMap<>();

    /*
     * Min-Heap ordered by:
     * 1) totalSales (ascending)
     * 2) productId (reverse lex for deterministic ordering)
     */
    private final PriorityQueue<ProductEntry> minHeap =
        new PriorityQueue<>((a, b) -> {
            if (a.totalSales != b.totalSales) {
                return a.totalSales - b.totalSales;
            }
            return b.productId.compareTo(a.productId);
        });

    CategoryTracker(int n) {
        this.N = n;
    }

    /*
     * PROCESS A SALE EVENT
     *
     * Example:
     * Product = "iphone"
     * Category = "electronics"
     * Quantity = 3
     *
     * Steps:
     * 1) Update total sales in map
     * 2) Remove old heap entry (if exists)
     * 3) Insert updated entry
     * 4) Evict smallest if heap exceeds N
     */
    void processSale(String productId, int quantity) {

        int oldSales = salesMap.getOrDefault(productId, 0);
        int newSales = oldSales + quantity;
        salesMap.put(productId, newSales);

        // Remove outdated heap entry if present
        minHeap.removeIf(p -> p.productId.equals(productId));

        // Insert updated entry
        minHeap.offer(new ProductEntry(productId, newSales));

        // Keep only top N
        if (minHeap.size() > N) {
            minHeap.poll();
        }
    }

    /*
     * RETURN TOP N PRODUCTS (SORTED DESCENDING)
     */
    List<String> getTopProducts() {
        List<ProductEntry> list = new ArrayList<>(minHeap);

        // Sort for final output
        list.sort((a, b) -> {
            if (a.totalSales != b.totalSales) {
                return b.totalSales - a.totalSales;
            }
            return a.productId.compareTo(b.productId);
        });

        List<String> result = new ArrayList<>();
        for (ProductEntry p : list) {
            result.add(p.productId);
        }
        return result;
    }
}

/* =============================================================================
   MAIN MANAGER: TOP PRODUCTS PER CATEGORY
   -----------------------------------------------------------------------------
   This is the class the interviewer actually cares about.
============================================================================= */
class TopProductsPerCategory {

    private final int N;

    /*
     * category → CategoryTracker
     * Each category is isolated and independently maintained
     */
    private final Map<String, CategoryTracker> categoryMap = new HashMap<>();

    public TopProductsPerCategory(int n) {
        this.N = n;
    }

    /*
     * PROCESS A SALE EVENT FROM STREAM
     *
     * Example event:
     * ("iphone", "electronics", 2)
     */
    public void processSale(String productId, String category, int quantity) {

        /*
         * If category doesn't exist yet:
         * - Create a new CategoryTracker
         * - Otherwise reuse existing one
         */
        categoryMap
            .computeIfAbsent(category, c -> new CategoryTracker(N))
            .processSale(productId, quantity);
    }

    /*
     * GET TOP N PRODUCTS FOR A CATEGORY
     */
    public List<String> getTopProducts(String category) {
        if (!categoryMap.containsKey(category)) {
            return Collections.emptyList();
        }
        return categoryMap.get(category).getTopProducts();
    }

    /*
     * MAIN METHOD (OPTIONAL DEMO)
     * Not required in interview, but useful for validation.
     */
    public static void main(String[] args) {
        TopProductsPerCategory tracker = new TopProductsPerCategory(2);

        tracker.processSale("iphone", "electronics", 3);
        tracker.processSale("ipad", "electronics", 2);
        tracker.processSale("macbook", "electronics", 5);
        tracker.processSale("iphone", "electronics", 4);

        tracker.processSale("nike-shoes", "fashion", 6);
        tracker.processSale("adidas-shoes", "fashion", 3);

        System.out.println(tracker.getTopProducts("electronics"));
        // Expected: [iphone, macbook]

        System.out.println(tracker.getTopProducts("fashion"));
        // Expected: [nike-shoes, adidas-shoes]
    }
}

/*
================================================================================
TIME & SPACE COMPLEXITY (INTERVIEW ANSWER)
--------------------------------------------------------------------------------
Let:
C = number of categories
P = products per category
N = top N

Per sale event:
- O(log N)

Get top N:
- O(N log N)

Space:
- O(total products + C × N)

================================================================================
FOLLOW-UP DISCUSSION (WHAT TO SAY IF ASKED)
--------------------------------------------------------------------------------
1) Large scale?
   - Shard by category
   - Aggregate sales via stream processors
   - Cache results

2) Sliding window?
   - Use time buckets + eviction

3) Approximate results?
   - Count-Min Sketch

================================================================================
ONE-LINE SUMMARY (MEMORIZE)
--------------------------------------------------------------------------------
"Top N per category is best solved by isolating categories and maintaining a
min-heap of size N for each category."

================================================================================
*/
