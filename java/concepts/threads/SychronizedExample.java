import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class SychronizedExample {
    public static void main(String[] args) throws InterruptedException {
        // example1();
        ExecutorService service = Executors.newFixedThreadPool(3);
        Counter counter = new Counter();
        IntStream.range(0, 2000)
                // .forEach(num -> service.submit(counter::syncIncrement));
                .forEach(num -> service.submit(counter::increment));
        service.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        service.shutdown();
        System.out.println(counter.getCount());
    }

    private static void example1() throws InterruptedException {
        Counter counter = new Counter();
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        });
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++)
                counter.increment();
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println("Counter value : " + counter.getCount());
    }
}

class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++; // not thread safe
    }

    public void syncIncrement() {
        synchronized (this) {
            count++; // thread safe
        }
    }

    public int getCount() {
        return count;
    }
}

class SharedResource {
    private static int count = 0;

    // ✔ Locking on Class: Synchronizing static methods locks the class
    // (SharedResource.class) instead of an instance.
    public static synchronized void increment() {
        count++;
    }

    public static int getCount() {
        return count;
    }
}
