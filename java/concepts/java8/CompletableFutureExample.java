import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureExample {
    public static void main(String[] args) {
        RunAsyncExample runAsyncExample = new RunAsyncExample();
        runAsyncExample.task();

        SupplyAsyncExample supplyAsyncExample = new SupplyAsyncExample();
        supplyAsyncExample.task();

        ChainingCompletableFutureExample chainingCompletableFutureExample = new ChainingCompletableFutureExample();
        chainingCompletableFutureExample.task();

        CombiningCompletableFuturesExample combiningCompletableFuturesExample = new CombiningCompletableFuturesExample();
        combiningCompletableFuturesExample.task();

        ComposeCompletableFuturesExample composeCompletableFuturesExample = new ComposeCompletableFuturesExample();
        composeCompletableFuturesExample.task();
    }
}

class RunAsyncExample {

    // ✅ Used for fire-and-forget operations.
    // ✅ It returns CompletableFuture<Void> and does not return any result.
    // ✅ It runs the task in a separate thread. ForkJoinPool.commonPool-worker-1

    // Uses Runnable interface - void run()
    public void task() {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("Running task in separate thread : " + Thread.currentThread().getName());
        });

        future.join();
    }
}

class SupplyAsyncExample {

    // ✅ Used for background tasks returning a value.
    // Uses Supplier interface - T get()

    public void task() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return "Running taks in separate thread : " + Thread.currentThread().getName();
        });

        System.out.println(future.join());
    }
}

class ChainingCompletableFutureExample {
    public void task() {
        CompletableFuture.supplyAsync(() -> {
            return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        }).thenApply(list -> {
            // thenApply Transforms the result without blocking.
            return list.stream().map(num -> num * num).toList();
        }).thenAccept(System.out::println)
                .thenRun(() -> {
                    System.out.println("Task completed");
                }); // thenRun Used for logging or side effects.

    }
}

class CombiningCompletableFuturesExample {
    public void task() {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Future1 : " + Thread.currentThread().getName());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Future1 completed");
            return 10;
        });

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Future2 : " + Thread.currentThread().getName());
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Future2 completed");
            return 20;
        });

        CompletableFuture<Integer> future3 = future1.thenCombine(future2, (num1, num2) -> num1 + num2);

        System.out.println(future3.join());
    }
}

class ComposeCompletableFuturesExample {
    public void task() {
        CompletableFuture.supplyAsync(() -> 100)
                .thenCompose(prevFutureResult -> CompletableFuture.supplyAsync(() -> prevFutureResult + 200))
                .thenAccept(System.out::println);
    }
}