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

    /**
     * Problem:
     * Given an array/list "pages" where pages[i] = number of pages in i-th book.
     * We must allocate books to "numberOfStudents" students such that:
     *
     * 1) Each student gets at least 1 book
     * 2) Books must be assigned in contiguous order (no reordering)
     * 3) We want to minimize the maximum pages assigned to any student
     *
     * Return the minimum possible value of the maximum pages.
     *
     * Example:
     * pages = [10, 20, 30, 40], students = 2
     *
     * Possible allocation:
     *   Student1: [10,20,30] -> 60 pages
     *   Student2: [40]       -> 40 pages
     *   max = 60
     *
     * Another:
     *   Student1: [10,20] -> 30 pages
     *   Student2: [30,40] -> 70 pages
     *   max = 70
     *
     * Minimum max is 60.
     *
     * ---------------------------------------------------------
     * Why Binary Search on Answer?
     *
     * We are asked to MINIMIZE the maximum pages.
     *
     * If we assume a limit X = maximum pages allowed per student,
     * we can check feasibility:
     *
     *   "Can we allocate all books to students such that no student
     *    gets more than X pages?"
     *
     * Feasibility is monotonic:
     *   - If X works, then any larger value X+1, X+2 also works.
     *   - If X does NOT work, then any smaller value also does not work.
     *
     * Pattern becomes:
     *   false false false true true true ...
     *
     * So we binary search the smallest feasible X.
     *
     * ---------------------------------------------------------
     * Search Space:
     * low  = max(pages)    (at least one student must take the largest book)
     * high = sum(pages)    (one student takes all books)
     *
     * Time Complexity:
     *   O(n log(sum(pages)))
     *
     * Space Complexity:
     *   O(1)
     */
    public int findPages(List<Integer> pages, int numberOfStudents) {

        // If students > books, impossible because each student needs at least 1 book
        if (numberOfStudents > pages.size()) {
            return -1;
        }

        // Minimum possible maximum pages = largest single book
        int low = Collections.max(pages);

        // Maximum possible maximum pages = sum of all books (one student reads all)
        int high = pages.stream().mapToInt(Integer::intValue).sum();

        // Binary search for the FIRST feasible maxPages value
        while (low <= high) {

            int mid = low + (high - low) / 2;

            // If we set maxPagesAllowed = mid,
            // compute how many students are required.
            int studentsNeeded = getNumberOfStudentsNeeded(pages, mid);

            // If we need more students than available,
            // it means mid is too small (not enough capacity per student).
            if (studentsNeeded > numberOfStudents) {
                low = mid + 1;
            }
            // Otherwise mid is feasible, but we want minimum, so try smaller.
            else {
                high = mid - 1;
            }
        }

        // low is the smallest feasible maximum pages allocation
        return low;
    }

    /**
     * Given a maximum limit "maxPages", calculate how many students are required
     * if we allocate books in order.
     *
     * Greedy approach:
     * - Keep adding books to current student until adding the next book exceeds maxPages.
     * - Then assign next book to a new student.
     *
     * This greedy strategy gives the MINIMUM number of students needed
     * for a given maxPages.
     *
     * Example:
     * pages = [10,20,30,40], maxPages = 60
     *
     * Student1: 10 + 20 + 30 = 60
     * Student2: 40
     * studentsNeeded = 2
     *
     * Time Complexity: O(n)
     */
    private int getNumberOfStudentsNeeded(List<Integer> pages, int maxPages) {

        int studentsNeeded = 1;   // start with first student
        int currentSum = 0;       // pages allocated to current student

        for (int pagesCount : pages) {

            // If we can add this book without exceeding maxPages, allocate it
            if (currentSum + pagesCount <= maxPages) {
                currentSum += pagesCount;
            }
            // Otherwise allocate this book to a new student
            else {
                studentsNeeded++;
                currentSum = pagesCount;
            }
        }

        return studentsNeeded;
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
