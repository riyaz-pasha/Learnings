/*
 * Problem Statement: Given an array ‘arr of integer numbers, ‘ar[i]’ represents
 * the number of pages in the ‘i-th’ book. There are a ‘m’ number of students,
 * and the task is to allocate all the books to the students.
 * Allocate books in such a way that:
 * 
 * Each student gets at least one book.
 * Each book should be allocated to only one student.
 * Book allocation should be in a contiguous manner.
 * You have to allocate the book to ‘m’ students such that the maximum number of
 * pages assigned to a student is minimum. If the allocation of books is not
 * possible. return -1
 * 
 * Examples
 * 
 * Example 1:
 * Input Format: n = 4, m = 2, arr[] = {12, 34, 67, 90}
 * Result: 113
 * Explanation: The allocation of books will be 12, 34, 67 | 90. One student
 * will get the first 3 books and the other will get the last one.
 * 
 * Example 2:
 * Input Format: n = 5, m = 4, arr[] = {25, 46, 28, 49, 24}
 * Result: 71
 * Explanation: The allocation of books will be 25, 46 | 28 | 49 | 24.
 */

import java.util.Collections;
import java.util.List;

class AllocateMinimumNumberOfPages {

    public int findPages(List<Integer> pages, int numberOfStudents) {
        if (numberOfStudents > pages.size()) {
            return -1;
        }

        int low = Collections.max(pages);
        int high = pages.stream().mapToInt(Integer::intValue).sum();

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int noOfStudentsNeeded = this.getNumberOfStudentsNeeded(pages, mid);
            if (noOfStudentsNeeded > numberOfStudents) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    private int getNumberOfStudentsNeeded(List<Integer> pages, int maxPages) {
        int noOfStudentsNeeded = 1;
        int currentStudentPagesCount = 0;
        for (int pagesCount : pages) {
            if (currentStudentPagesCount + pagesCount <= maxPages) {
                currentStudentPagesCount += pagesCount;
            } else {
                noOfStudentsNeeded++;
                currentStudentPagesCount = pagesCount;
            }
        }

        return noOfStudentsNeeded;
    }

}

class BookAllocation {

    public static boolean canAllocate(int[] arr, int m, int maxPages) {
        int studentCount = 1;
        int pagesSum = 0;

        for (int pages : arr) {
            if (pages > maxPages)
                return false; // Single book is larger than mid

            if (pagesSum + pages > maxPages) {
                studentCount++;
                pagesSum = pages;

                if (studentCount > m)
                    return false;
            } else {
                pagesSum += pages;
            }
        }

        return true;
    }

    public static int findMinimumPages(int[] arr, int m) {
        int n = arr.length;
        if (m > n)
            return -1;

        int low = arr[0], high = 0;
        for (int pages : arr) {
            low = Math.max(low, pages);
            high += pages;
        }

        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (canAllocate(arr, m, mid)) {
                result = mid; // Try for a better minimum
                high = mid - 1;
            } else {
                low = mid + 1; // Increase the max pages per student
            }
        }

        return result;
    }

    public static void main(String[] args) {
        int[] books1 = { 12, 34, 67, 90 };
        int m1 = 2;
        System.out.println(findMinimumPages(books1, m1)); // Output: 113

        int[] books2 = { 25, 46, 28, 49, 24 };
        int m2 = 4;
        System.out.println(findMinimumPages(books2, m2)); // Output: 71
    }

}

/*
 * Algorithm:
 * 
 * If m > n: In this case, book allocation is not possible and so, we will
 * return -1.
 * 
 * Place the 2 pointers i.e. low and high: Initially, we will place the
 * pointers. The pointer low will point to max(arr[]) and the high will point to
 * sum(arr[]).
 * 
 * Calculate the ‘mid’: Now, inside the loop, we will calculate the value of
 * ‘mid’ using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * 
 * Eliminate the halves based on the number of students returned by
 * countStudents():
 * We will pass the potential number of pages, represented by the variable
 * 'mid', to the ‘countStudents()' function. This function will return the
 * number of students to whom we can allocate the books.
 * 
 * - If students > m: On satisfying this condition, we can conclude that the
 * number ‘mid’ is smaller than our answer. So, we will eliminate the left half
 * and consider the right half(i.e. low = mid+1).
 * 
 * - Otherwise, the value mid is one of the possible answers. But we want the
 * minimum value. So, we will eliminate the right half and consider the left
 * half(i.e. high = mid-1).
 * 
 * Finally, outside the loop, we will return the value of low as the pointer
 * will be pointing to the answer.
 * 
 * Time Complexity: O(N * log(sum(arr[])-max(arr[])+1)), where N = size of the
 * array, sum(arr[]) = sum of all array elements, max(arr[]) = maximum of all
 * array elements.
 * Reason: We are applying binary search on [max(arr[]), sum(arr[])]. Inside the
 * loop, we are calling the countStudents() function for the value of ‘mid’.
 * Now, inside the countStudents() function, we are using a loop that runs for N
 * times.
 * 
 * Space Complexity: O(1) as we are not using any extra space to solve this
 * problem.
 */
