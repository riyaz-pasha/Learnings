
import java.util.Arrays;

/*
 * Problem Statement: You are given an array of integers 'arr' and an integer
 * i.e. a threshold value 'limit'. Your task is to find the smallest positive
 * integer divisor, such that upon dividing all the elements of the given array
 * by it, the sum of the division's result is less than or equal to the given
 * threshold value.
 */

class FindTheSmallestDivisor {

    public int smallestDivisor(int[] nums, int limit) {
        int low = 1, high = Arrays.stream(nums).max().getAsInt();
        int result = high;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (this.sumOfDivisors(nums, mid) <= limit) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return result;
    }

    private int sumOfDivisors(int[] nums, int div) {
        int sum = 0;
        for (int num : nums) {
            sum += Math.ceil((double) num / div);
        }
        return sum;
    }

    public int smallestDivisor2(int[] nums, int limit) {
        int low = 1, high = Arrays.stream(nums).max().getAsInt();

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (this.sumOfDivisors(nums, mid) <= limit) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return low;
    }

}

/*
 * Algorithm:
 * If n > threshold: If the minimum summation i.e. n > threshold value, the
 * answer does not exist. In this case, we will return -1.
 * 
 * Next, we will find the maximum element i.e. max(arr[]) in the given array.
 * 
 * Place the 2 pointers i.e. low and high: Initially, we will place the
 * pointers. The pointer low will point to 1 and the high will point to
 * max(arr[]).
 * 
 * Calculate the ‘mid’: Now, inside the loop, we will calculate the value of
 * ‘mid’ using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * 
 * Eliminate the halves based on the summation of division results:
 * We will pass the potential divisor, represented by the variable 'mid', to the
 * 'sumByD()' function. This function will return the summation result of the
 * division values.
 * 
 * - If result <= threshold: On satisfying this condition, we can conclude that
 * the number ‘mid’ is one of our possible answers. But we want the minimum
 * number. So, we will eliminate the right half and consider the left half(i.e.
 * high = mid-1).
 * 
 * - Otherwise, the value mid is smaller than the number we want. This means the
 * numbers greater than ‘mid’ should be considered and the right half of ‘mid’
 * consists of such numbers. So, we will eliminate the left half and consider
 * the right half(i.e. low = mid+1).
 * 
 * Finally, outside the loop, we will return the value of low as the pointer
 * will be pointing to the answer.
 * 
 * Time Complexity: O(log(max(arr[]))*N), where max(arr[]) = maximum element in
 * the array, N = size of the array.
 * Reason: We are applying binary search on our answers that are in the range of
 * [1, max(arr[])]. For every possible divisor ‘mid’, we call the sumByD()
 * function. Inside that function, we are traversing the entire array, which
 * results in O(N).
 * 
 * Space Complexity: O(1) as we are not using any extra space to solve this
 * problem.
 */
