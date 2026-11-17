import java.util.ArrayDeque;
import java.util.Queue;

class MovingAverage {

    private final int size;
    private final Queue<Integer> window;
    private long sum;

    public MovingAverage(int size) {
        this.size = size;
        this.window = new ArrayDeque<>();
        this.sum = 0;
    }

    public double next(int val) {

        // STEP 1: If the window is full, remove the oldest element.
        if (window.size() == size) {

            // FIFO behavior ensures that the oldest element
            // leaves the window first.
            int removed = window.poll();

            // Maintain the running sum by removing its contribution.
            sum -= removed;
        }

        // STEP 2: Insert the incoming element.
        window.offer(val);

        // Update the running sum in O(1).
        sum += val;

        // STEP 3: Calculate the average.
        // window.size() automatically handles the initial phase
        // when fewer than 'size' elements have arrived.
        //
        // Casting to double is important:
        // Otherwise integer division would truncate decimal values.
        //
        // Time Complexity: O(1) per next() call.
        // Queue insertion, removal and size operations are O(1).
        //
        // Space Complexity: O(k).
        // The queue never contains more than k elements.
        return (double) sum / window.size();
    }
}





















class MovingAverage {

    private final int[] window;

    // Points to the position where the next incoming value
    // should be inserted.
    private int nextIndex;

    // Number of elements currently stored in the window.
    // Never exceeds window.length.
    private int count;

    // Sum of all elements currently present in the window.
    private long sum;

    public MovingAverage(int size) {

        // Fixed-size array avoids dynamic allocation and
        // queue object overhead.
        this.window = new int[size];

        this.nextIndex = 0;
        this.count = 0;
        this.sum = 0;
    }

    public double next(int val) {

        // STEP 1: Remove the contribution of the element
        // that is about to be overwritten.
        //
        // Before the buffer becomes full, array positions
        // contain no valid elements, so we must not subtract
        // their default zero values as actual observations.
        if (count == window.length) {
            sum -= window[nextIndex];
        }

        // STEP 2: Insert the new value.
        window[nextIndex] = val;

        // Update the running sum.
        sum += val;

        // STEP 3: Update the number of valid elements.
        //
        // Once full, count remains equal to window.length.
        count = Math.min(count + 1, window.length);

        // STEP 4: Move the insertion pointer forward.
        //
        // Modulo ensures that we wrap around to index 0
        // after reaching the last array position.
        nextIndex = (nextIndex + 1) % window.length;

        // STEP 5: Calculate the moving average.
        //
        // During initialization, divide by the number of
        // elements actually received.
        // Once full, divide by the fixed window size.
        //
        // Time Complexity: O(1) per next() call.
        // Only a constant number of array operations occur.
        //
        // Space Complexity: O(k).
        // A fixed array of exactly k elements is maintained.
        return (double) sum / count;
    }
}

