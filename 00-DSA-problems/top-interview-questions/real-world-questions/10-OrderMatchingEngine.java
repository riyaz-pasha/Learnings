import java.util.PriorityQueue;

class Order {

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

}

class BuyOrder extends Order implements Comparable<BuyOrder> {

    public BuyOrder(int quantity, double price) {
        super(quantity, price);
    }

    @Override
    public int compareTo(BuyOrder other) {
        if (this.price == other.price) {
            return Long.compare(this.timestamp, other.timestamp); // FIFO
        }
        return Double.compare(other.price, this.price); // Max-heap for buy orders
    }

}

class SellOrder extends Order implements Comparable<SellOrder> {

    public SellOrder(int quantity, double price) {
        super(quantity, price);
    }

    @Override
    public int compareTo(SellOrder other) {
        if (this.price == other.price) {
            return Long.compare(this.timestamp, other.timestamp); // FIFO
        }
        return Double.compare(this.price, other.price); // Min-heap for sell orders

    }

}

class OrderMatchingEngine {

    PriorityQueue<BuyOrder> buyOrders = new PriorityQueue<>();
    PriorityQueue<SellOrder> sellOrders = new PriorityQueue<>();

    public void placeBuyOrder(BuyOrder buyOrder) {
        this.matchBuyOrder(buyOrder);
    }

    public void placeSellOrder(SellOrder sellOrder) {
        this.matchSellOrder(sellOrder);
    }

    private void matchBuyOrder(BuyOrder buyOrder) {
        while (buyOrder.quantity > 0 && !this.sellOrders.isEmpty() && this.sellOrders.peek().price <= buyOrder.price) {
            SellOrder sellOrder = this.sellOrders.poll();
            int tradedQuantity = Math.min(buyOrder.quantity, sellOrder.quantity);
            buyOrder.quantity -= tradedQuantity;
            sellOrder.quantity -= tradedQuantity;
            if (sellOrder.quantity > 0) {
                this.sellOrders.offer(sellOrder); // reinsert if partially filled
            }
        }

        if (buyOrder.quantity > 0) {
            this.buyOrders.offer(buyOrder);
        }
    }

    private void matchSellOrder(SellOrder sellOrder) {
        while (sellOrder.quantity > 0 && !this.buyOrders.isEmpty() && this.buyOrders.peek().price >= sellOrder.price) {
            BuyOrder buyOrder = this.buyOrders.poll();
            int tradedQuantity = Math.min(buyOrder.quantity, sellOrder.quantity);
            buyOrder.quantity -= tradedQuantity;
            sellOrder.quantity -= tradedQuantity;

            if (buyOrder.quantity > 0) {
                this.buyOrders.offer(buyOrder);
            }
        }

        if (sellOrder.quantity > 0) {
            this.sellOrders.offer(sellOrder);
        }
    }

}

/*
 * Buy Orders (Bids): These are orders from buyers specifying the maximum price
 * they are willing to pay for a certain quantity of an asset.
 * 
 * Sell Orders (Asks/Offers): These are orders from sellers specifying the
 * minimum price they are willing to accept for a certain quantity.
 * 
 * The core of your implementation is the matching logic, which runs when a new
 * order arrives. For a Price-Time Priority algorithm, the logic would look
 * something like this:
 * 
 * Receive a new order: A new buy or sell order is submitted to the system.
 * 
 * Check for a match:
 * 
 * - For a new Buy Order: Check the top (lowest-priced) sell orders in the order
 * book. If the new buy order's price is greater than or equal to the lowest ask
 * price, a match can occur.
 * 
 * - For a new Sell Order: Check the top (highest-priced) buy orders in the
 * order book. If the new sell order's price is less than or equal to the
 * highest bid price, a match can occur.
 * 
 * Execute the trade: If a match is found, create a trade object, update the
 * quantities of the matched orders, and remove any fully filled orders from the
 * order book.
 * 
 * Loop and continue matching: If the new order is only partially filled, repeat
 * the matching process with the remaining quantity against the next available
 * orders in the order book until it's fully filled or no more matches can be
 * made.
 * 
 * Place the order in the book: If the new order is not fully filled, place the
 * remaining quantity into the appropriate position in the order book based on
 * its price and arrival time.
 */

/*
 * 1. Placing an order (buy or sell)
 * Each new order may:
 * - Peek the top of the opposite heap → O(1)
 * - Poll from the opposite heap → O(log N)
 * - Offer (reinsert partially filled orders) → O(log N)
 * - Insert into own heap if not matched fully → O(log N)
 * 
 * In the worst-case, you may need to:
 * - Match against multiple opposite orders (linear scan of heap, one by one)
 * - So if there are M orders matched, each takes O(log N) to remove/reinsert
 * 
 * ➤ So:
 * 🔸 Worst-case per order:
 * O(M × log N)
 * Where:
 * 
 * M = number of orders matched (can be ≤ N)
 * N = number of orders in the heap
 * 
 * 🔸 Amortized/typical case:
 * Often just 1-2 matches → close to:
 * 
 * O(log N)
 * 
 * 2. Peeking Best Match
 * O(1) from priority queue
 * 
 * 3. Matching One Pair
 * poll() + offer() = O(log N)
 * Matching logic itself = O(1)
 * 
 * ✅ Space Complexity
 * You maintain:
 * A PriorityQueue<BuyOrder> and PriorityQueue<SellOrder>
 * 
 * Each order is stored once, so:
 * Space = O(N)
 * Where N is the total number of unmatched buy + sell orders in the system.
 * 
 */
