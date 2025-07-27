class SimpleCLass {

    public static void main(String[] args) {
        rec();
    }

    public static void rec() {
        int a = 2;
        int b = 3;
        int c = a + b;
        int d = c + 2;
        // rec();
    }

    public int num() {
        return 1;
    }

    public Integer num2() {
        return 1;
    }

    public Integer num3() {
        Integer val = Integer.valueOf(1);
        return val;
    }

    public float floatNum() {
        return 1;
    }

    public String str(String upper) {
        return upper.toLowerCase();
    }

}
