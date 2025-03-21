import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SingleThreadFutureExample {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CubeCalculator cubeCalculator = new CubeCalculator(executor);

        Future<Integer> future = cubeCalculator.cube(2);
        System.out.println("Waiting for result future1...");
        System.out.println("Result : " + future.get()); // blocking

        Future<Integer> future2 = cubeCalculator.cube(3);

        while (!future2.isDone()) {
            System.out.println("Waiting for result of future2...");
            Thread.sleep(300);
        }
        System.out.println("Result : " + future2.get());

        Future<Integer> future3 = cubeCalculator.cube(3);
        System.out.println("Waiting for result of future3...");
        future3.cancel(true); // Attempt to cancel the task

        System.out.println("Is future3 task cancelled? " + future3.isCancelled());
        executor.shutdown();
    }
}

class CubeCalculator {

    private ExecutorService executor;

    CubeCalculator(ExecutorService executor) {
        this.executor = executor;
    }

    public Future<Integer> cube(Integer num) {
        return executor.submit(() -> {
            Thread.sleep(1000);
            return num * num * num;
        });
    }
}
