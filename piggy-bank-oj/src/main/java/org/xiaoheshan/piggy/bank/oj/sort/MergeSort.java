package org.xiaoheshan.piggy.bank.oj.sort;

public class MergeSort {

    public static void sort(int[] array) {
        if (array == null || array.length == 0) {
            return;
        }
        int[] temp = new int[array.length];
        sort(array, 0, array.length - 1, temp);
    }

    private static void sort(int[] array, int start, int end, int[] temp) {
        if (start >= end) {
            return;
        }
        int middle = (start + end)/ 2;
        sort(array, start, middle, temp);
        sort(array, middle + 1, end, temp);
        merge(array, start, middle, end, temp);
    }

    private static void merge(int[] array, int start, int middle, int end, int[] temp) {
        int i = start;
        int j = middle + 1;
        int k = 0;
        while(i <= middle && j <= end) {
            if (array[i] < array[j]) {
                temp[k++] = array[i++];
            } else {
                temp[k++] = array[j++];
            }
        }

        while (i <= middle) {
            temp[k++] = array[i++];
        }
        while (j <= end) {
            temp[k++] = array[j++];
        }
        for (int kk = start; kk <= end; kk++) {
            array[kk] = temp[kk - start];
        }
    }
}
