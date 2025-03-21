public class DeadLockExample {
    public static void main(String[] args) {
        // example1();

        Shared s1 = new Shared();
        Shared s2 = new Shared();

        Thread t1 = new Thread(() -> s1.method1(s2), "Thread1");
        Thread t2 = new Thread(() -> s2.method1(s1), "Thread2");

        t1.start();
        t2.start();
    }

    private static void example1() {
        Resource resource1 = new Resource();
        Resource resource2 = new Resource();

        Thread thread1 = new Thread(() -> resource1.method(resource2), "Thread-1");
        Thread thread2 = new Thread(() -> resource2.method(resource1), "Thread-2");

        thread1.start();
        thread2.start();
    }
}

class Resource {
    void method(Resource resource) {
        synchronized (this) {
            System.out.println(Thread.currentThread().getName() + " locked method");
            synchronized (resource) {
                System.out.println(Thread.currentThread().getName() + " locked method");
            }
        }
    }
}

class Shared {
    synchronized void method1(Shared other) {
        System.out.println(Thread.currentThread().getName() + " locked " + this);
        Util.sleep(500);
        System.out.println(Thread.currentThread().getName() + " waiting for " + other);
        other.method2();
    }

    synchronized void method2() {
        System.out.println(Thread.currentThread().getName() + " locked " + this);
    }
}

class Util {
    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}