public class ThreadCreation {
    public static void main(String[] args) {
        ThreadClassExtensionExample threadClassExtensionExample = new ThreadClassExtensionExample();
        threadClassExtensionExample.start();

        ThreadInterfaceImplementationExample threadInterfaceImplementationExample = new ThreadInterfaceImplementationExample();
        Thread thread = new Thread(threadInterfaceImplementationExample);
        thread.start();
    }
}

class ThreadClassExtensionExample extends Thread {
    public void run() {
        System.out.println("ThreadClassExtensionExample is running : " + Thread.currentThread().getName());
    }
}

class ThreadInterfaceImplementationExample implements Runnable {
    public void run() {
        System.out.println("ThreadInterfaceImplementationExample is running : " + Thread.currentThread().getName());
    }
}
