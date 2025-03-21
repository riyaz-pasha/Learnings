public class ThreadLifeCycleExample {
    public static void main(String[] args) throws InterruptedException {
        // blockedState();
        // waiting();
        // timedWaiting();
    }

    private static void timedWaiting() throws InterruptedException {
        Thread thread1 = new Thread(new TimedWaitingState());
        thread1.start();
        Thread.sleep(1000);
        System.out.println("Thread1 state : " + thread1.getState());
    }

    private static void waiting() {
        WaitingState1.thread1 = new Thread(new WaitingState1());
        System.out.println("Thread1 state : " + WaitingState1.thread1.getState());
        WaitingState1.thread1.start();
        System.out.println("Thread1 state after start: " + WaitingState1.thread1.getState());
    }

    private static void blockedState() {
        Thread thread1 = new Thread(new BlockedState());
        Thread thread2 = new Thread(new BlockedState());

        System.out.println("Thread1 state : " + thread1.getState());
        System.out.println("Thread2 state : " + thread2.getState());

        thread1.start();
        thread2.start();

        System.out.println("Thread1 state after start: " + thread1.getState());
        System.out.println("Thread2 state after start: " + thread2.getState());
        // Thread.sleep(1000);

        System.out.println("Thread1 state: " + thread1.getState());
        System.out.println("Thread2 state: " + thread2.getState());
    }
}

class BlockedState implements Runnable {

    @Override
    public void run() {
        commonResource();
    }

    public static synchronized void commonResource() {
        for (int i = 0; i < 5; i++) {
            // try {
            // Thread.sleep(1000);
            // } catch (InterruptedException e) {
            // Thread.currentThread().interrupt(); // Restore interrupted status
            // System.out.println("Thread was interrupted");
            // }
            System.out.println(Thread.currentThread().getName() + " : " + i);
        }
    }
}

class WaitingState2 implements Runnable {

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            System.out.println("Thread was interrupted");
        }
        System.out.println("Thread1 state : " + WaitingState1.thread1.getState());
    }
}

class WaitingState1 implements Runnable {

    public static Thread thread1;

    @Override
    public void run() {
        Thread thread2 = new Thread(new WaitingState2());
        System.out.println("Thread2 state : " + thread2.getState());
        thread2.start();
        System.out.println("Thread2 state after start: " + thread2.getState());

        try {
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            System.out.println("Thread was interrupted");
        }

        System.out.println("Thread2 state after join: " + thread2.getState());
    }
}

class TimedWaitingState implements Runnable {
    @Override
    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}