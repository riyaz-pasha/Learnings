class Tribonacci {
    public int tribonacci(int n) {
        if (n <= 1) return n;
        if (n == 2) return 1;
        int last3 = 0;
        int last2 = 1;
        int last1 = 1;
        for (int i = 3; i <= n; i++) {
            int temp = last1 + last2 + last3;
            last3 = last2;
            last2 = last1;
            last1 = temp;
        }
        return last1;
    }
}
