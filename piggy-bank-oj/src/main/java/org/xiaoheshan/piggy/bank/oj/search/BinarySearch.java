package org.xiaoheshan.piggy.bank.oj.search;

public class BinarySearch {


    private int search(int[] array, int target) {
        if (array == null || array.length == 0) {
            return -1;
        }
        int left = 0;
        int right = array.length - 1;
        while (left <= right) {
            int middle = left + (right - left) >> 1;
            if (array[middle] > target) {
                left = middle + 1;
            } else if (array[middle] < ) {
                right = middle - 1;
            } else {
                return middle;
            }
        }
        return -1;
    }
}
