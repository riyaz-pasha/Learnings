class LambdaExample {
    public static void main(String[] args) {
        LambdaBefore lambdaBefore = new LambdaBefore();
        lambdaBefore.add();
        LambdaAfter lambdaAfter = new LambdaAfter();
        lambdaAfter.add();
    }
}

interface Add {
    int apply(int a, int b);
}

class LambdaBefore {
    public void add() {
        Add add = new Add() {
            public int apply(int a, int b) {
                return a + b;
            }
        };
        System.out.println(add.apply(2, 3));
    }
}

class LambdaAfter {
    public void add() {
        Add add = (int a, int b) -> a + b;
        System.out.println(add.apply(2, 3));
    }
}
