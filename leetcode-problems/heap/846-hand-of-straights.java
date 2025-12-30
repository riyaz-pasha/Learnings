import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * PROBLEM ANALYSIS:
 * -----------------
 * Alice needs to arrange cards into groups where:
 * 1. Each group has exactly 'groupSize' cards
 * 2. Each group contains consecutive values (e.g., [1,2,3] or [5,6,7,8])
 * 
 * KEY INSIGHTS:
 * 1. Total cards must be divisible by groupSize (otherwise impossible)
 * 2. We need to form groups starting from the smallest available card
 * 3. Once we start a group with card value X, we MUST find X+1, X+2, ..., X+groupSize-1
 * 4. Greedy approach works: always try to form groups starting with smallest card
 * 
 * APPROACH:
 * - Use a frequency map to count occurrences of each card
 * - Sort the unique card values
 * - For each card (starting from smallest), try to form a group
 * - Decrement frequencies as we form groups
 */

class CardGrouping {
    
    /**
     * SOLUTION 1: TreeMap Approach (Most Intuitive)
     * Time Complexity: O(n log n) - TreeMap operations
     * Space Complexity: O(n) - storing card frequencies
     * 
     * REASONING:
     * - TreeMap keeps keys sorted automatically
     * - We can always access the smallest remaining card efficiently
     * - Remove entries when frequency becomes 0 to avoid processing them again
     */
    public boolean isNStraightHand_TreeMap(int[] hand, int groupSize) {
        // Edge case: if total cards not divisible by groupSize, impossible
        if (hand.length % groupSize != 0) {
            return false;
        }
        
        // TreeMap maintains sorted order of keys (card values)
        // Key: card value, Value: frequency of that card
        TreeMap<Integer, Integer> cardCount = new TreeMap<>();
        
        // Count frequency of each card
        for (int card : hand) {
            cardCount.put(card, cardCount.getOrDefault(card, 0) + 1);
        }
        
        // Process cards from smallest to largest
        while (!cardCount.isEmpty()) {
            // Get the smallest card value (TreeMap.firstKey() gives min key)
            int firstCard = cardCount.firstKey();
            
            // Try to form a group starting with this card
            // Group needs: firstCard, firstCard+1, firstCard+2, ..., firstCard+groupSize-1
            for (int i = 0; i < groupSize; i++) {
                int currentCard = firstCard + i;
                
                // If this card doesn't exist, we can't form a complete group
                if (!cardCount.containsKey(currentCard)) {
                    return false;
                }
                
                // Use one instance of this card
                int count = cardCount.get(currentCard);
                
                if (count == 1) {
                    // Last instance of this card, remove from map
                    cardCount.remove(currentCard);
                } else {
                    // Decrement the count
                    cardCount.put(currentCard, count - 1);
                }
            }
        }
        
        return true;
    }
    
    /**
     * SOLUTION 2: HashMap + Sorting Approach
     * Time Complexity: O(n log n) - dominated by sorting
     * Space Complexity: O(n) - for storing frequencies and unique values
     * 
     * REASONING:
     * - More efficient than TreeMap for large datasets
     * - Sort once upfront instead of maintaining sorted structure
     * - Use array to iterate through sorted unique values
     */
    public boolean isNStraightHand_Sorted(int[] hand, int groupSize) {
        if (hand.length % groupSize != 0) {
            return false;
        }
        
        // Count frequencies using HashMap (faster than TreeMap for insertions)
        Map<Integer, Integer> cardCount = new HashMap<>();
        for (int card : hand) {
            cardCount.put(card, cardCount.getOrDefault(card, 0) + 1);
        }
        
        // Extract unique card values and sort them
        // We need sorted order to process from smallest to largest
        int[] uniqueCards = new int[cardCount.size()];
        int idx = 0;
        for (int card : cardCount.keySet()) {
            uniqueCards[idx++] = card;
        }
        Arrays.sort(uniqueCards);
        
        // Process each unique card value
        for (int card : uniqueCards) {
            int count = cardCount.get(card);
            
            // Skip if already used up in previous groups
            if (count == 0) {
                continue;
            }
            
            // We need to form 'count' groups starting with this card
            // Why? Because this is the smallest remaining card, and each
            // instance of it must start a new group
            for (int i = 0; i < groupSize; i++) {
                int currentCard = card + i;
                int currentCount = cardCount.getOrDefault(currentCard, 0);
                
                // If we don't have enough of currentCard to form 'count' groups
                if (currentCount < count) {
                    return false;
                }
                
                // Use 'count' instances of currentCard
                cardCount.put(currentCard, currentCount - count);
            }
        }
        
        return true;
    }
    
    /**
     * SOLUTION 3: Priority Queue (Min Heap) Approach
     * Time Complexity: O(n log n) - heap operations
     * Space Complexity: O(n)
     * 
     * REASONING:
     * - Similar to TreeMap but uses PriorityQueue
     * - Good for understanding heap-based solutions
     * - Slightly less efficient than TreeMap due to duplicate removals
     */
    public boolean isNStraightHand_PriorityQueue(int[] hand, int groupSize) {
        if (hand.length % groupSize != 0) {
            return false;
        }
        
        // Count frequencies
        Map<Integer, Integer> cardCount = new HashMap<>();
        for (int card : hand) {
            cardCount.put(card, cardCount.getOrDefault(card, 0) + 1);
        }
        
        // Min heap to always get the smallest card
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(cardCount.keySet());
        
        while (!minHeap.isEmpty()) {
            int firstCard = minHeap.peek();
            
            // Try to form a group
            for (int i = 0; i < groupSize; i++) {
                int currentCard = firstCard + i;
                
                if (!cardCount.containsKey(currentCard) || cardCount.get(currentCard) == 0) {
                    return false;
                }
                
                int count = cardCount.get(currentCard);
                cardCount.put(currentCard, count - 1);
                
                // Remove from heap if count becomes 0
                if (count - 1 == 0) {
                    // IMPORTANT: We must remove the exact card that became 0
                    // If currentCard != firstCard, heap structure may have this card deeper
                    // So we specifically remove currentCard
                    minHeap.remove(currentCard);
                }
            }
        }
        
        return true;
    }
    
    /**
     * TEST CASES
     */
    public static void main(String[] args) {
        CardGrouping solution = new CardGrouping();
        
        // Test Case 1: Basic valid case
        int[] hand1 = {1, 2, 3, 6, 2, 3, 4, 7, 8};
        int groupSize1 = 3;
        System.out.println("Test 1 (TreeMap): " + solution.isNStraightHand_TreeMap(hand1, groupSize1)); // true
        System.out.println("Test 1 (Sorted): " + solution.isNStraightHand_Sorted(hand1, groupSize1)); // true
        System.out.println("Test 1 (PQ): " + solution.isNStraightHand_PriorityQueue(hand1, groupSize1)); // true
        
        // Test Case 2: Invalid case - not enough cards
        int[] hand2 = {1, 2, 3, 4, 5};
        int groupSize2 = 4;
        System.out.println("\nTest 2 (TreeMap): " + solution.isNStraightHand_TreeMap(hand2, groupSize2)); // false
        System.out.println("Test 2 (Sorted): " + solution.isNStraightHand_Sorted(hand2, groupSize2)); // false
        System.out.println("Test 2 (PQ): " + solution.isNStraightHand_PriorityQueue(hand2, groupSize2)); // false
        
        // Test Case 3: Gap in sequence
        int[] hand3 = {1, 2, 3, 5, 6, 7};
        int groupSize3 = 3;
        System.out.println("\nTest 3 (TreeMap): " + solution.isNStraightHand_TreeMap(hand3, groupSize3)); // false
        System.out.println("Test 3 (Sorted): " + solution.isNStraightHand_Sorted(hand3, groupSize3)); // false
        System.out.println("Test 3 (PQ): " + solution.isNStraightHand_PriorityQueue(hand3, groupSize3)); // false
        
        // Test Case 4: All same cards
        int[] hand4 = {1, 1, 1, 1, 1, 1};
        int groupSize4 = 3;
        System.out.println("\nTest 4 (TreeMap): " + solution.isNStraightHand_TreeMap(hand4, groupSize4)); // false
        System.out.println("Test 4 (Sorted): " + solution.isNStraightHand_Sorted(hand4, groupSize4)); // false
        System.out.println("Test 4 (PQ): " + solution.isNStraightHand_PriorityQueue(hand4, groupSize4)); // false
        
        // Test Case 5: Single group
        int[] hand5 = {1, 2, 3};
        int groupSize5 = 3;
        System.out.println("\nTest 5 (TreeMap): " + solution.isNStraightHand_TreeMap(hand5, groupSize5)); // true
        System.out.println("Test 5 (Sorted): " + solution.isNStraightHand_Sorted(hand5, groupSize5)); // true
        System.out.println("Test 5 (PQ): " + solution.isNStraightHand_PriorityQueue(hand5, groupSize5)); // true
    }
}

/**
 * COMPLEXITY COMPARISON:
 * ----------------------
 * All three solutions have O(n log n) time complexity, but with differences:
 * 
 * TreeMap: O(n log n) for insertions + O(k log n) for removals where k = unique cards
 * Sorted:  O(n log n) for sorting + O(n) for processing (best overall)
 * PQ:      O(n log n) for operations + O(k²) for remove operations (worst)
 * 
 * RECOMMENDED: Solution 2 (Sorted approach) for best performance
 * 
 * EDGE CASES HANDLED:
 * 1. hand.length not divisible by groupSize
 * 2. Cards with gaps in sequence
 * 3. Insufficient cards to complete groups
 * 4. Duplicate cards
 * 5. Single group
 * 6. Empty hand (would return true as 0 % groupSize == 0)
 */
