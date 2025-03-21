import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockExample {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        }, "Thread-1");
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        }, "Thread-2");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println("Count: " + counter.getCount());
    }
}

class Counter {
    private int count = 0;
    private ReentrantLock reentrantLock = new ReentrantLock();

    public void increment() {
        reentrantLock.lock();
        try {
            count++;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void sageIncrement() {
        if (reentrantLock.tryLock()) {
            try {
                System.out.println(Thread.currentThread().getName() + " acquired the lock");
                count++;
                System.out.println(Thread.currentThread().getName() + " incremented the count");
            } finally {
                reentrantLock.unlock();
            }
        } else {
            System.out.println(Thread.currentThread().getName() + " couldn't acquire lock");
        }
    }

    public int getCount() {
        return count;
    }
}
