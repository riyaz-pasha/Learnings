class Fibonacci {
    public int fib(int n) {
        if (n <= 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }
}

class FibonacciDP {
    // bottom-up
    public int fib(int n) {
        int[] cache = new int[n + 2];
        cache[0] = 0;
        cache[1] = 1;
        for (int i = 2; i <= n; i++) {
            cache[i] = cache[i - 1] + cache[i - 2];
        }
        return cache[n];
    }

    public int fib2(int n) {
        if (n <= 1) return n;
        int last2 = 0;
        int last1 = 1;
        for (int i = 2; i <= n; i++) {
            int current = last1 + last2;
            last2 = last1;
            last1 = current;
        }
        return last1;
    }
}
