
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class NetWorthCalculator {

    public BigDecimal calculateNetWorth(
            Map<String, BigDecimal> wallets,
            Map<String, BigDecimal> exchangeRates, // assuming everything is mapping to base currency
            String baseCurrency) {
        BigDecimal totalNetWorth = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> wallet : wallets.entrySet()) {
            String currency = wallet.getKey();
            BigDecimal amount = wallet.getValue();

            if (currency.equals(baseCurrency)) {
                totalNetWorth = totalNetWorth.add(amount);
            } else {
                if (exchangeRates.containsKey(currency)) {
                    BigDecimal exchangeRate = exchangeRates.get(currency);
                    BigDecimal convertedAmount = amount.multiply(exchangeRate);

                    totalNetWorth = totalNetWorth.add(convertedAmount);
                } else {
                    throw new IllegalArgumentException("Missing exchange rate for currency: " + currency);
                }
            }
        }

        return totalNetWorth;
    }

    // Main method for testing
    public static void main(String[] args) {
        NetWorthCalculator calculator = new NetWorthCalculator();

        Map<String, BigDecimal> wallets = new HashMap<>();
        wallets.put("USD", new BigDecimal("1000.50"));
        wallets.put("EUR", new BigDecimal("500.25"));
        wallets.put("JPY", new BigDecimal("20000.75"));

        Map<String, BigDecimal> exchangeRates = new HashMap<>();
        // Assuming USD is the base currency
        exchangeRates.put("EUR", new BigDecimal("1.0825"));
        exchangeRates.put("JPY", new BigDecimal("0.006571"));

        String baseCurrency = "USD";

        BigDecimal netWorth = calculator.calculateNetWorth(wallets, exchangeRates, baseCurrency);
        System.out.println("Total Net Worth in " + baseCurrency + ": " + netWorth);
    }

    /*
     * Time Complexity: O(N), where N is the number of wallets. The operations for
     * BigDecimal (add, multiply) are not strictly constant time, but for the
     * typical number of digits in financial amounts, they can be considered
     * constant for complexity analysis purposes. The dominant factor is still the
     * loop over N wallets.
     * 
     * Space Complexity: O(1). We are using a BigDecimal object for the total, which
     * is a fixed-size reference. The space does not scale with the input size. The
     * input data structures themselves (the maps) take O(N) space, but the
     * additional space used by the algorithm is constant.
     */

}

// --------------------------------------------------------------------------------------------------

enum currency {
}

class Wallet {

    Currency currency;
    BigDecimal amount;

    Wallet(Currency currency, BigDecimal amount) {
        this.currency = currency;
        this.amount = amount;
    }

}

class ExchangeRate {

    Currency from;
    Currency to;
    BigDecimal rate;

    ExchangeRate(Currency from, Currency to, BigDecimal rate) {
        this.from = from;
        this.to = to;
        this.rate = rate;
    }

}

class Pair {

    Currency currency;
    BigDecimal rateToBase;

    Pair(Currency currency, BigDecimal rateToBase) {
        this.currency = currency;
        this.rateToBase = rateToBase;
    }

}

class CurrencyConverter {

    public BigDecimal calculateNetWorth(List<Wallet> wallets, List<ExchangeRate> exchangeRates, Currency baseCurrency) {

        Map<Currency, List<Pair>> graph = new HashMap<>();
        for (ExchangeRate rate : exchangeRates) {
            graph.computeIfAbsent(rate.from, _ -> new ArrayList<>())
                    .add(new Pair(rate.to, rate.rate));
            graph.computeIfAbsent(rate.to, _ -> new ArrayList<>())
                    .add(new Pair(rate.from, BigDecimal.ONE.divide(rate.rate)));
        }

        // Dijkstra from base currency
        Map<Currency, BigDecimal> rateToBase = new HashMap<>();
        PriorityQueue<Pair> pq = new PriorityQueue<>(Comparator.comparing(p -> p.rateToBase));

        pq.offer(new Pair(baseCurrency, BigDecimal.ONE));
        rateToBase.put(baseCurrency, BigDecimal.ONE);

        while (!pq.isEmpty()) {
            Pair current = pq.poll();

            for (Pair neighbor : graph.getOrDefault(current.currency, new ArrayList<>())) {
                BigDecimal newRate = current.rateToBase.multiply(neighbor.rateToBase);
                if (!rateToBase.containsKey(neighbor.currency)
                        || newRate.compareTo(rateToBase.get(neighbor.currency)) < 0) {
                    rateToBase.put(neighbor.currency, newRate);
                    pq.offer(new Pair(neighbor.currency, newRate));
                }
            }
        }

        return this.calculateNetWorthInBaseCurrency(wallets, rateToBase, baseCurrency);
    }

    public BigDecimal calculateNetWorthInBaseCurrency(
            List<Wallet> wallets,
            Map<Currency, BigDecimal> exchangeRates, // assuming everything is mapping to base currency
            Currency baseCurrency) {
        BigDecimal totalNetWorth = BigDecimal.ZERO;

        for (Wallet wallet : wallets) {
            Currency currency = wallet.currency;
            BigDecimal amount = wallet.amount;

            if (currency.equals(baseCurrency)) {
                totalNetWorth = totalNetWorth.add(amount);
            } else {
                if (exchangeRates.containsKey(currency)) {
                    BigDecimal exchangeRate = exchangeRates.get(currency);
                    BigDecimal convertedAmount = amount.multiply(exchangeRate);

                    totalNetWorth = totalNetWorth.add(convertedAmount);
                } else {
                    throw new IllegalArgumentException("Missing exchange rate for currency: " + currency);
                }
            }
        }

        return totalNetWorth;
    }

    /*
     * ⏱️ Time Complexity:
     * Graph Build: O(E)
     * Dijkstra: O((V+E) log V) using PriorityQueue
     * Total: O(E + V log V + N)
     * 
     * 📦 Space Complexity:
     * O(V + E) for graph + distances
     */

}
