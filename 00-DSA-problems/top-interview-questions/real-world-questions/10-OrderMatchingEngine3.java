/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Match BUYERS and SELLERS in real time with BEST prices using a
PRICE-LEVEL GROUPED ORDER BOOK.

Each order:
- orderId
- BUY / SELL
- price
- quantity

Rules:
1) BUY matches with LOWEST available SELL price
2) SELL matches with HIGHEST available BUY price
3) Orders at SAME PRICE are matched FIFO
4) Partial fills allowed

================================================================================
INTERVIEW FLOW (WHAT I SAY OUT LOUD)
--------------------------------------------------------------------------------
Step 1: Clarify
- Real-time stream of orders
- Partial fills allowed
- FIFO within same price
- Best price priority

Step 2: Key Insight
- Heap of individual orders is inefficient at scale
- Orders should be grouped by PRICE LEVEL
- Maintain sorted price levels + FIFO queue per level

Step 3: Data Structures
BUY side:
- TreeMap<price, Queue<Order>> (descending prices)

SELL side:
- TreeMap<price, Queue<Order>> (ascending prices)

TreeMap gives:
- O(log P) access to best price level
- FIFO maintained via Queue

================================================================================
*/

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;

/* =============================================================================
   ENUMS & MODELS
============================================================================= */

enum Side {
    BUY, SELL
}

class Order {
    String orderId;
    Side side;
    double price;
    int quantity;

    Order(String orderId, Side side, double price, int quantity) {
        this.orderId = orderId;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }
}

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
        return "TRADE [BUY=" + buyOrderId +
               ", SELL=" + sellOrderId +
               ", PRICE=" + price +
               ", QTY=" + quantity + "]";
    }
}

/* =============================================================================
   PRICE LEVEL ORDER BOOK
============================================================================= */
class PriceLevelOrderBook {

    /*
     * BUY BOOK:
     * - Highest price first
     * - price -> FIFO queue of orders
     */
    private final TreeMap<Double, Deque<Order>> buyBook =
        new TreeMap<>(Collections.reverseOrder());

    /*
     * SELL BOOK:
     * - Lowest price first
     * - price -> FIFO queue of orders
     */
    private final TreeMap<Double, Deque<Order>> sellBook =
        new TreeMap<>();

    /*
     * PROCESS INCOMING ORDER
     */
    public List<Trade> processOrder(Order order) {
        List<Trade> trades = new ArrayList<>();

        if (order.side == Side.BUY) {
            matchBuy(order, trades);
        } else {
            matchSell(order, trades);
        }

        return trades;
    }

    /*
     * MATCH BUY ORDER
     */
    private void matchBuy(Order buyOrder, List<Trade> trades) {

        while (buyOrder.quantity > 0 && !sellBook.isEmpty()) {

            double bestSellPrice = sellBook.firstKey();

            // No price match possible
            if (bestSellPrice > buyOrder.price) break;

            Deque<Order> sellQueue = sellBook.get(bestSellPrice);
            Order sellOrder = sellQueue.peekFirst();

            int matchedQty = Math.min(buyOrder.quantity, sellOrder.quantity);

            trades.add(new Trade(
                buyOrder.orderId,
                sellOrder.orderId,
                bestSellPrice,
                matchedQty
            ));

            buyOrder.quantity -= matchedQty;
            sellOrder.quantity -= matchedQty;

            if (sellOrder.quantity == 0) {
                sellQueue.pollFirst();
                if (sellQueue.isEmpty()) {
                    sellBook.remove(bestSellPrice);
                }
            }
        }

        if (buyOrder.quantity > 0) {
            buyBook
                .computeIfAbsent(buyOrder.price, p -> new ArrayDeque<>())
                .offerLast(buyOrder);
        }
    }

    /*
     * MATCH SELL ORDER
     */
    private void matchSell(Order sellOrder, List<Trade> trades) {

        while (sellOrder.quantity > 0 && !buyBook.isEmpty()) {

            double bestBuyPrice = buyBook.firstKey();

            // No price match possible
            if (bestBuyPrice < sellOrder.price) break;

            Deque<Order> buyQueue = buyBook.get(bestBuyPrice);
            Order buyOrder = buyQueue.peekFirst();

            int matchedQty = Math.min(sellOrder.quantity, buyOrder.quantity);

            trades.add(new Trade(
                buyOrder.orderId,
                sellOrder.orderId,
                bestBuyPrice,
                matchedQty
            ));

            sellOrder.quantity -= matchedQty;
            buyOrder.quantity -= matchedQty;

            if (buyOrder.quantity == 0) {
                buyQueue.pollFirst();
                if (buyQueue.isEmpty()) {
                    buyBook.remove(bestBuyPrice);
                }
            }
        }

        if (sellOrder.quantity > 0) {
            sellBook
                .computeIfAbsent(sellOrder.price, p -> new ArrayDeque<>())
                .offerLast(sellOrder);
        }
    }

    /*
     * DEBUG VIEW
     */
    public void printOrderBook() {
        System.out.println("BUY BOOK:");
        buyBook.forEach((price, q) ->
            System.out.println(price + " -> " + q.size() + " orders")
        );

        System.out.println("SELL BOOK:");
        sellBook.forEach((price, q) ->
            System.out.println(price + " -> " + q.size() + " orders")
        );
    }

    /*
     * DEMO
     */
    public static void main(String[] args) {
        PriceLevelOrderBook book = new PriceLevelOrderBook();

        book.processOrder(new Order("B1", Side.BUY, 100, 10));
        book.processOrder(new Order("S1", Side.SELL, 95, 5));
        book.processOrder(new Order("S2", Side.SELL, 98, 10));
        book.processOrder(new Order("B2", Side.BUY, 97, 8));

        book.printOrderBook();
    }
}
