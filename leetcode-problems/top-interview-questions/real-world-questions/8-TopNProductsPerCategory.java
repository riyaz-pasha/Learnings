/* Given a product stream, return top N products per category based on sales. */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.TreeSet;

class Product {
}

class Category {
}

class Sale {

    Product product;
    Category category;
    int sales;

}

class ProductSales implements Comparable<ProductSales> {

    Product product;
    int totalSales;

    ProductSales(Product product, int totalSales) {
        this.product = product;
        this.totalSales = totalSales;
    }

    @Override
    public int compareTo(ProductSales other) {
        return Integer.compare(this.totalSales, other.totalSales);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProductSales other = (ProductSales) obj;
        if (product == null) {
            if (other.product != null)
                return false;
        } else if (!product.equals(other.product))
            return false;
        return true;
    }

}

class TopNProductsPerCategory {

    Map<Category, Map<Product, Integer>> categoryTotalSalesMap = new HashMap<>();

    /*
     * Time:
     * O(1) for updating the Map<Category, Map<Product, Integer>>.
     * 
     * Space:
     * O(C × P), where:
     * C = number of unique categories
     * P = number of unique products per category
     */
    public void recordSale(Sale sale) {
        Map<Product, Integer> productTotalSalesMap = this.categoryTotalSalesMap
                .computeIfAbsent(sale.category, k -> new HashMap<>());
        int totalSales = productTotalSalesMap.getOrDefault(sale.product, 0) + sale.sales;
        productTotalSalesMap.put(sale.product, totalSales);
    }

    /*
     * Time:
     * Let M = number of products in the category
     * For each product, push into a min-heap of size N:
     * O(M log N)
     * 
     * Space (temporary):
     * O(N) for the min-heap used during the query
     * Plus the permanent O(C × P) storage in the map.
     */
    public List<ProductSales> getTopNProducts(Category category, int n) {
        Map<Product, Integer> productTotalSalesMap = this.categoryTotalSalesMap
                .computeIfAbsent(category, k -> new HashMap<>());

        PriorityQueue<ProductSales> minHeap = new PriorityQueue<>();

        for (Map.Entry<Product, Integer> entry : productTotalSalesMap.entrySet()) {
            minHeap.offer(new ProductSales(entry.getKey(), entry.getValue()));
            if (minHeap.size() > n) {
                minHeap.poll();
            }
        }

        List<ProductSales> topSalesProducts = new LinkedList<>();
        while (!minHeap.isEmpty()) {
            topSalesProducts.add(minHeap.poll());
        }
        Collections.reverse(topSalesProducts);

        return topSalesProducts;
    }

}

class TopNProductsPerCategoryV2 {

    int n;
    Map<Product, ProductSales> productTotalSalesMap = new HashMap<>();
    Map<Category, TreeSet<ProductSales>> categoryTotalSalesMap = new HashMap<>();

    /*
     * Time:
     * Remove from TreeSet → O(log N)
     * Update count → O(1)
     * Add back to TreeSet → O(log N)
     * Evict lowest → O(log N) (in worst case)
     * ✅ Total per update: O(log N)
     * 
     * Space:
     * O(P) for productTotalSalesMap (global product tracking)
     * O(C × N) for category-wise top-N TreeSets
     * ✅ Total space: O(P + C × N)
     */
    public void recordSale(Sale sale) {
        ProductSales productSales = this.productTotalSalesMap
                .computeIfAbsent(sale.product, k -> new ProductSales(sale.product, 0));

        TreeSet<ProductSales> topSalesOfCategory = this.categoryTotalSalesMap
                .computeIfAbsent(sale.category, k -> new TreeSet<>());

        topSalesOfCategory.remove(productSales);

        productSales.totalSales += sale.sales;

        topSalesOfCategory.add(productSales);

        if (topSalesOfCategory.size() > n) {
            topSalesOfCategory.pollLast();
        }
    }

    /*
     * Time:
     * O(N) — simply copy from TreeSet
     * 
     * Space:
     * Temporary list of size N: O(N)
     */
    public List<ProductSales> getTopNProducts(Category category) {
        return new ArrayList<>(this.categoryTotalSalesMap.getOrDefault(category, new TreeSet<>()));
    }

}
