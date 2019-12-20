package org.xiaoheshan.piggy.bank.oj.sort;

public class InsertionSort {

    public static void sort(int[] array) {
        if (array == null || array.length == 0) {
            return;
        }
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = i + 1; j > 0; j--) {
                if (array[j] < array[j-1]) {
                    int tem = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = tem;
                } else {
                    break;
                }
            }
        }
    }
}
