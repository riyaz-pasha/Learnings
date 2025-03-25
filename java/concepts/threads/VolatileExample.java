public class VolatileExample {

    public static void main(String[] args) throws InterruptedException {
        // VolatileProblemSimulation.run();
        // VolatileFixedSimulation.run();
        // new SharedResourceProblemSimulation().run();
        new SharedResourceSolvedSimulation().run();
    }
}

class VolatileProblemSimulation {
    private static boolean flag = false; // No volatile

    public static void run() throws InterruptedException {
        Thread reader = new Thread(() -> {
            System.out.println("Reader thread started...");
            while (!flag) { // Might loop forever due to caching
            }
            System.out.println("Reader thread detected flag = true!");
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(2000); // Delay to simulate real-world scenario
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Writer thread updating flag...");
            flag = true; // Update flag
        });

        reader.start();
        writer.start();
    }

    // The reader thread may keep looping forever, even though flag is set to true.
    // This happens because flag is stored in CPU cache, and the reader thread never
    // sees the updated value in main memory.

}

class VolatileFixedSimulation {
    private static volatile boolean flag = false; // Now volatile

    public static void run() throws InterruptedException {
        Thread reader = new Thread(() -> {
            System.out.println("Reader thread started...");
            while (!flag) { // Always reads latest value
            }
            System.out.println("Reader thread detected flag = true!");
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Writer thread updating flag...");
            flag = true; // Now visible to all threads
        });

        reader.start();
        writer.start();
    }
}

class SharedResourceProblemSimulation {
    private boolean flag = false; // No volatile

    public void setFlagTrue() {
        flag = true;
    }

    public boolean isFlagSet() {
        return flag;
    }

    public void run() {
        Thread reader = new Thread(() -> {
            System.out.println("Reader thread started...");
            while (!isFlagSet()) {
            } // Might loop forever!
            System.out.println("Reader detected flag = true!");
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            setFlagTrue(); // Update flag
            System.out.println("Writer updated flag.");
        });

        reader.start();
        writer.start();
    }
}

class SharedResourceSolvedSimulation {
    private volatile boolean flag = false; // volatile

    public void setFlagTrue() {
        flag = true;
    }

    public boolean isFlagSet() {
        return flag;
    }

    public void run() {
        Thread reader = new Thread(() -> {
            System.out.println("Reader thread started...");
            while (!isFlagSet()) {
            } // Might loop forever!
            System.out.println("Reader detected flag = true!");
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            setFlagTrue(); // Update flag
            System.out.println("Writer updated flag.");
        });

        reader.start();
        writer.start();
    }
}
