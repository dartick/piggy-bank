package org.xiaoheshan.piggy.bank.oj.sort;

public class QuickSort {

    public static void sort(int[] array) {
        sortInternal(array, 0, array.length - 1);
    }

    private static void sortInternal(int[] array, int start, int end) {
        if (start >= end) {
            return;
        }
        int left = start;
        int right = end;
        int key = array[left];

        while (left < right) {
            while (left < right && array[right] >= key) {
                right--;
            }
            if (left < right ) {
                array[left] = array[right];
                left++;
            }
            while (left < right && array[left] < key) {
                left++;
            }
            if (left < right) {
                array[right] = array[left];
                right--;
            }
        }

        array[left] = key;
        sortInternal(array, start, left - 1);
        sortInternal(array, left + 1, end);
    }
}
