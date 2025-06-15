/*
 * Implement the RandomizedSet class:
 * 
 * RandomizedSet() Initializes the RandomizedSet object.
 * 
 * bool insert(int val) Inserts an item val into the set if not present. Returns
 * true if the item was not present, false otherwise.
 * 
 * bool remove(int val) Removes an item val from the set if present. Returns
 * true if the item was present, false otherwise.
 * 
 * int getRandom() Returns a random element from the current set of elements
 * (it's guaranteed that at least one element exists when this method is
 * called). Each element must have the same probability of being returned.
 * 
 * You must implement the functions of the class such that each function works
 * in average O(1) time complexity.
 * 
 */

import java.util.*;

class RandomizedSet {

    // HashMap to store value -> index mapping for O(1) lookup
    private final Map<Integer, Integer> valueToIndex;

    // ArrayList to store actual values for O(1) random access
    private final List<Integer> values;

    // Random object for generating random indices
    private final Random random;

    public RandomizedSet() {
        valueToIndex = new HashMap<>();
        values = new ArrayList<>();
        random = new Random();
    }

    /**
     * Inserts a value to the set. Returns true if the set did not already contain
     * the specified element.
     * Time Complexity: O(1) average
     */
    public boolean insert(int val) {
        // If value already exists, return false
        if (valueToIndex.containsKey(val)) {
            return false;
        }

        // Add value to end of list
        values.add(val);

        // Store the mapping of value to its index
        valueToIndex.put(val, values.size() - 1);

        return true;
    }

    /**
     * Removes a value from the set. Returns true if the set contained the specified
     * element.
     * Time Complexity: O(1) average
     */
    public boolean remove(int val) {
        // If value doesn't exist, return false
        if (!valueToIndex.containsKey(val)) {
            return false;
        }

        // Get the index of the value to remove
        int indexToRemove = valueToIndex.get(val);
        int lastIndex = values.size() - 1;

        // If the element to remove is not the last element
        if (indexToRemove != lastIndex) {
            // Get the last element
            int lastElement = values.get(lastIndex);

            // Move the last element to the position of element to remove
            values.set(indexToRemove, lastElement);

            // Update the index mapping for the moved element
            valueToIndex.put(lastElement, indexToRemove);
        }

        // Remove the last element from the list
        values.remove(lastIndex);

        // Remove the value from the map
        valueToIndex.remove(val);

        return true;
    }

    /**
     * Get a random element from the set.
     * Time Complexity: O(1)
     */
    public int getRandom() {
        // Generate random index and return the element at that index
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    // Additional utility methods for testing
    public int size() {
        return values.size();
    }

    public boolean contains(int val) {
        return valueToIndex.containsKey(val);
    }

    public void printSet() {
        System.out.println("Current set: " + values);
        System.out.println("Index mapping: " + valueToIndex);
    }
}

// Test class to demonstrate the functionality
class RandomizedSetTest {

    public static void main(String[] args) {
        RandomizedSet randomizedSet = new RandomizedSet();

        System.out.println("=== Testing RandomizedSet ===\n");

        // Test insertions
        System.out.println("Inserting 1: " + randomizedSet.insert(1)); // true
        System.out.println("Inserting 2: " + randomizedSet.insert(2)); // true
        System.out.println("Inserting 3: " + randomizedSet.insert(3)); // true
        System.out.println("Inserting 1 again: " + randomizedSet.insert(1)); // false (duplicate)

        randomizedSet.printSet();
        System.out.println();

        // Test getRandom
        System.out.println("Getting random elements:");
        for (int i = 0; i < 10; i++) {
            System.out.print(randomizedSet.getRandom() + " ");
        }
        System.out.println("\n");

        // Test removal
        System.out.println("Removing 2: " + randomizedSet.remove(2)); // true
        randomizedSet.printSet();
        System.out.println();

        System.out.println("Removing 2 again: " + randomizedSet.remove(2)); // false (not present)
        System.out.println("Removing 4: " + randomizedSet.remove(4)); // false (never existed)

        // Test more operations
        System.out.println("Inserting 4: " + randomizedSet.insert(4)); // true
        System.out.println("Inserting 5: " + randomizedSet.insert(5)); // true
        randomizedSet.printSet();
        System.out.println();

        System.out.println("Final random elements:");
        for (int i = 0; i < 10; i++) {
            System.out.print(randomizedSet.getRandom() + " ");
        }
        System.out.println("\n");

        // Test edge cases
        System.out.println("=== Edge Case Testing ===");
        RandomizedSet singleElementSet = new RandomizedSet();
        singleElementSet.insert(42);
        System.out.println("Single element set - getRandom(): " + singleElementSet.getRandom());
        System.out.println("Removing single element: " + singleElementSet.remove(42));

        // Test with larger dataset
        System.out.println("\n=== Performance Test ===");
        RandomizedSet largeSet = new RandomizedSet();

        // Insert 1000 elements
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            largeSet.insert(i);
        }
        long insertTime = System.nanoTime() - startTime;

        // Get 1000 random elements
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            largeSet.getRandom();
        }
        long randomTime = System.nanoTime() - startTime;

        // Remove 500 elements
        startTime = System.nanoTime();
        for (int i = 0; i < 500; i++) {
            largeSet.remove(i);
        }
        long removeTime = System.nanoTime() - startTime;

        System.out.println("Insert 1000 elements: " + insertTime / 1000000.0 + " ms");
        System.out.println("Get 1000 random elements: " + randomTime / 1000000.0 + " ms");
        System.out.println("Remove 500 elements: " + removeTime / 1000000.0 + " ms");
        System.out.println("Final set size: " + largeSet.size());
    }

}

/*
 * ALGORITHM EXPLANATION:
 * 
 * Data Structures Used:
 * 1. HashMap<Integer, Integer> valueToIndex: Maps each value to its index in
 * the ArrayList
 * 2. ArrayList<Integer> values: Stores the actual values for O(1) random access
 * 3. Random random: For generating random indices
 * 
 * Key Operations:
 * 
 * INSERT:
 * - Check if value exists in HashMap - O(1)
 * - If not exists: add to end of ArrayList and update HashMap - O(1)
 * - Return appropriate boolean
 * 
 * REMOVE (The Tricky One):
 * - Check if value exists - O(1)
 * - If exists:
 * -- Get index of element to remove
 * -- Move last element to this position (to avoid shifting)
 * -- Update HashMap for the moved element
 * -- Remove last element from ArrayList
 * -- Remove from HashMap
 * - All operations are O(1)
 * 
 * GET_RANDOM:
 * - Generate random index between 0 and size-1
 * - Return element at that index - O(1)
 * 
 * Why This Works:
 * - HashMap gives O(1) lookup to check existence
 * - ArrayList gives O(1) random access by index
 * - The remove trick (swap with last element) avoids O(n) shifting
 * - Each element has equal probability of being selected
 * 
 * Space Complexity: O(n) where n is the number of elements
 * 
 * The key insight for the remove operation is that we don't need to maintain
 * the original order of elements. By swapping the element to remove with the
 * last element, we can remove in O(1) time while maintaining the ability
 * to access any element randomly.
 * 
 * Example walkthrough:
 * values = [1, 2, 3, 4], valueToIndex = {1:0, 2:1, 3:2, 4:3}
 * Remove 2:
 * 1. Find index of 2: index = 1
 * 2. Move last element (4) to index 1: values = [1, 4, 3, 4]
 * 3. Update mapping: valueToIndex = {1:0, 4:1, 3:2}
 * 4. Remove last element: values = [1, 4, 3]
 * 5. Remove 2 from map: valueToIndex = {1:0, 4:1, 3:2}
 */
