/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Match BUYERS and SELLERS in real time with the BEST possible prices.

Each order:
- orderId
- side: BUY or SELL
- price
- quantity

Matching rules:
1) BUY wants the LOWEST possible SELL price
2) SELL wants the HIGHEST possible BUY price
3) A match happens when:
   bestBuyPrice >= bestSellPrice
4) Orders can be partially filled
5) Remaining quantity stays in the market

================================================================================
INTERVIEW FLOW (WHAT TO SAY OUT LOUD)
--------------------------------------------------------------------------------
Step 1: Clarify requirements
- Orders arrive as a stream
- Matching must be real-time
- Partial fills allowed
- Best price wins, FIFO within same price

Step 2: Key observation
- We always want:
  - Highest BUY price
  - Lowest SELL price

Step 3: Data structure choice
- MaxHeap for BUY orders (highest price first)
- MinHeap for SELL orders (lowest price first)

This is a classic ORDER BOOK design.

Step 4: Complexity target
- Insert order: O(log N)
- Match orders: O(log N)
- Works efficiently for real-time systems

================================================================================
*/

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/* =============================================================================
   ORDER TYPE ENUM
============================================================================= */
enum OrderType {
    BUY,
    SELL
}

/* =============================================================================
   ORDER MODEL
============================================================================= */
class Order {
    String orderId;
    OrderType type;
    double price;
    int quantity;

    Order(String orderId, OrderType type, double price, int quantity) {
        this.orderId = orderId;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }
}

/* =============================================================================
   TRADE RESULT (OPTIONAL, FOR OUTPUT / LOGGING)
============================================================================= */
class Trade {
    String buyOrderId;
    String sellOrderId;
    double price;
    int quantity;

    Trade(String buyOrderId, String sellOrderId, double price, int quantity) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "TRADE -> Buy:" + buyOrderId +
               " Sell:" + sellOrderId +
               " Price:" + price +
               " Qty:" + quantity;
    }
}

/* =============================================================================
   MATCHING ENGINE (CORE CLASS)
============================================================================= */
class OrderMatchingEngine {

    /*
     * BUY orders:
     * - Highest price has priority
     */
    private final PriorityQueue<Order> buyBook =
        new PriorityQueue<>((a, b) -> Double.compare(b.price, a.price));

    /*
     * SELL orders:
     * - Lowest price has priority
     */
    private final PriorityQueue<Order> sellBook =
        new PriorityQueue<>((a, b) -> Double.compare(a.price, b.price));

    /*
     * PROCESS INCOMING ORDER
     */
    public List<Trade> processOrder(Order order) {
        List<Trade> trades = new ArrayList<>();

        if (order.type == OrderType.BUY) {
            matchBuyOrder(order, trades);
        } else {
            matchSellOrder(order, trades);
        }

        return trades;
    }

    /*
     * MATCH BUY ORDER
     *
     * BUY wants the lowest priced SELL
     */
    private void matchBuyOrder(Order buyOrder, List<Trade> trades) {

        while (buyOrder.quantity > 0 &&
               !sellBook.isEmpty() &&
               sellBook.peek().price <= buyOrder.price) {

            Order sellOrder = sellBook.peek();

            int matchedQty = Math.min(buyOrder.quantity, sellOrder.quantity);

            trades.add(new Trade(
                buyOrder.orderId,
                sellOrder.orderId,
                sellOrder.price,   // trade happens at SELL price
                matchedQty
            ));

            buyOrder.quantity -= matchedQty;
            sellOrder.quantity -= matchedQty;

            // Remove sell order if fully filled
            if (sellOrder.quantity == 0) {
                sellBook.poll();
            }
        }

        // If BUY order still has remaining quantity, add to order book
        if (buyOrder.quantity > 0) {
            buyBook.offer(buyOrder);
        }
    }

    /*
     * MATCH SELL ORDER
     *
     * SELL wants the highest priced BUY
     */
    private void matchSellOrder(Order sellOrder, List<Trade> trades) {

        while (sellOrder.quantity > 0 &&
               !buyBook.isEmpty() &&
               buyBook.peek().price >= sellOrder.price) {

            Order buyOrder = buyBook.peek();

            int matchedQty = Math.min(sellOrder.quantity, buyOrder.quantity);

            trades.add(new Trade(
                buyOrder.orderId,
                sellOrder.orderId,
                buyOrder.price,   // trade happens at BUY price
                matchedQty
            ));

            sellOrder.quantity -= matchedQty;
            buyOrder.quantity -= matchedQty;

            // Remove buy order if fully filled
            if (buyOrder.quantity == 0) {
                buyBook.poll();
            }
        }

        // If SELL order still has remaining quantity, add to order book
        if (sellOrder.quantity > 0) {
            sellBook.offer(sellOrder);
        }
    }

    /*
     * OPTIONAL: VIEW CURRENT ORDER BOOK STATE
     */
    public void printOrderBook() {
        System.out.println("BUY BOOK:");
        for (Order o : buyBook) {
            System.out.println(o.orderId + " @ " + o.price + " qty=" + o.quantity);
        }

        System.out.println("SELL BOOK:");
        for (Order o : sellBook) {
            System.out.println(o.orderId + " @ " + o.price + " qty=" + o.quantity);
        }
    }

    /*
     * DEMO MAIN METHOD
     */
    public static void main(String[] args) {
        OrderMatchingEngine engine = new OrderMatchingEngine();

        engine.processOrder(new Order("B1", OrderType.BUY, 100.0, 10));
        engine.processOrder(new Order("S1", OrderType.SELL, 95.0, 5));
        engine.processOrder(new Order("S2", OrderType.SELL, 98.0, 10));
        engine.processOrder(new Order("B2", OrderType.BUY, 97.0, 8));

        engine.printOrderBook();
    }
}

/*
================================================================================
TIME & SPACE COMPLEXITY (INTERVIEW ANSWER)
--------------------------------------------------------------------------------
Let N be total orders in book.

Insert order:     O(log N)
Match operation:  O(log N) per fill
Space:            O(N)

================================================================================
FOLLOW-UP QUESTIONS (BE READY)
--------------------------------------------------------------------------------
1) FIFO at same price?
   → Use LinkedList per price level (price-time priority)

2) Cancel order?
   → Keep Map<OrderId, Order> + lazy removal

3) Distributed system?
   → Partition by instrument, shard order books

4) High-frequency trading?
   → Lock-free queues, batching, native memory

================================================================================
ONE-LINE SUMMARY (MEMORIZE)
--------------------------------------------------------------------------------
"Real-time matching is best handled using two heaps: a max-heap for buyers and a
min-heap for sellers."

================================================================================
*/
