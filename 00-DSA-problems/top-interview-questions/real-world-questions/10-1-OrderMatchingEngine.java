import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

abstract class Order {

    static int globalId = 0;
    int id;
    int quantity;
    double price;
    long timestamp;

    public Order(int quantity, double price) {
        this.id = ++globalId;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = System.nanoTime();
    }

    @Override
    public String toString() {
        return getType() + " " + quantity + " @ " + price;
    }

    abstract String getType();

}

class BuyOrder extends Order {

    public BuyOrder(int quantity, double price) {
        super(quantity, price);
    }

    @Override
    String getType() {
        return "BUY";
    }

}

class SellOrder extends Order {

    public SellOrder(int quantity, double price) {
        super(quantity, price);
    }

    @Override
    String getType() {
        return "SELL";
    }

}

class OrderMatchingEngine {

    // Price -> FIFO queue of orders at that price

    // highest first
    // TreeMap maintains sorted price levels:
    // - Buy book uses reverse order (max-heap behavior)
    private final TreeMap<Double, Queue<BuyOrder>> buyBook = new TreeMap<>(Collections.reverseOrder());
    // - Sell book uses natural order (min-heap behavior)
    private final TreeMap<Double, Queue<SellOrder>> sellBook = new TreeMap<>();

    public void placeBuyOrder(BuyOrder buyOrder) {
        this.matchBuyOrder(buyOrder);
    }

    public void placeSellOrder(SellOrder sellOrder) {
        this.matchSellOrder(sellOrder);
    }

    private void matchBuyOrder(BuyOrder buyOrder) {
        Iterator<Entry<Double, Queue<SellOrder>>> it = this.sellBook.entrySet().iterator();

        while (buyOrder.quantity > 0 && it.hasNext()) {
            Entry<Double, Queue<SellOrder>> entry = it.next();
            double sellPrice = entry.getKey();

            if (sellPrice > buyOrder.price) {
                break;
            }

            Queue<SellOrder> sellQueue = entry.getValue();

            while (!sellQueue.isEmpty() && buyOrder.quantity > 0) {
                SellOrder sellOrder = sellQueue.peek();

                int tradedQuantity = Math.min(buyOrder.quantity, sellOrder.quantity);
                buyOrder.quantity -= tradedQuantity;
                sellOrder.quantity -= tradedQuantity;

                if (sellOrder.quantity == 0) {
                    sellQueue.poll();
                }
            }

            if (sellQueue.isEmpty()) {
                it.remove();
            }
        }

        if (buyOrder.quantity > 0) {
            this.buyBook.computeIfAbsent(buyOrder.price, k -> new LinkedList<>())
                    .add(buyOrder);
        }
    }

    private void matchSellOrder(SellOrder sellOrder) {
        Iterator<Entry<Double, Queue<BuyOrder>>> it = this.buyBook.entrySet().iterator();

        while (sellOrder.quantity > 0 && it.hasNext()) {
            Entry<Double, Queue<BuyOrder>> entry = it.next();
            double buyPrice = entry.getKey();

            if (sellOrder.price > buyPrice) {
                break;
            }

            Queue<BuyOrder> buyQueue = entry.getValue();

            while (!buyQueue.isEmpty() && sellOrder.quantity > 0) {
                BuyOrder buyOrder = buyQueue.peek();

                int tradedQuantity = Math.min(buyOrder.quantity, sellOrder.quantity);
                buyOrder.quantity -= tradedQuantity;
                sellOrder.quantity -= tradedQuantity;

                if (buyOrder.quantity == 0) {
                    buyQueue.poll();
                }
            }

            if (buyQueue.isEmpty()) {
                it.remove();
            }
        }

        if (sellOrder.quantity > 0) {
            this.sellBook.computeIfAbsent(sellOrder.price, k -> new LinkedList<>())
                    .add(sellOrder);
        }
    }

}

/*
 * ✅ Problems With PriorityQueue in High-Frequency Scenarios
 * No direct access to price levels (you must always poll the top).
 * Cannot batch match orders at the same price efficiently.
 * Poor performance when modifying/removing non-top orders.
 * No grouping by price → cannot see market depth cleanly.
 * 
 * ✅ Better Alternative: Price-Level Grouped Order Book
 * Use a TreeMap<Double, Queue<Order>>:
 * TreeMap gives sorted access to price levels
 * Queue maintains FIFO for fairness
 * Matching becomes price-level based, not per-order
 */

/*
 * TIME COMPLEXITY ANALYSIS:
 *
 * Let:
 * P = number of unique price levels in the opposite order book
 * Q = number of orders at a given price level
 * T = total number of unmatched orders in the system
 *
 * 1. placeBuyOrder / placeSellOrder:
 * - Worst-case: O(P * Q + log P)
 * (match through multiple price levels and insert remaining)
 * - Typical-case: O(log P)
 * (1-2 price levels matched)
 *
 * 2. matchBuyOrder / matchSellOrder:
 * - Matching 1 pair: O(1)
 * - Removing empty queue: O(log P)
 *
 * 3. Inserting unmatched order:
 * - TreeMap insert: O(log P)
 * - Queue add: O(1)
 *
 * 4. Order book traversal (printOrderBook):
 * - O(T) total where T = total unmatched orders
 *
 * SPACE COMPLEXITY:
 *
 * - O(T) total space where T = unmatched orders
 * - Each order stored once in a queue
 * - One queue per price level (up to P levels)
 *
 */
