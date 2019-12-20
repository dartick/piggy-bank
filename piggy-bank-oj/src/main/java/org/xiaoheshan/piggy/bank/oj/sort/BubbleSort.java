package org.xiaoheshan.piggy.bank.oj.sort;

public class BubbleSort {

    public static void sort(int[] array) {
        if (array == null || array.length == 0) {
            return;
        }
        for (int i = 0; i < array.length; i++) {
            for (int j = 1; j <= array.length - 1 - i; j++) {
                if (array[j - 1] >= array[j]) {
                     int tem = array[j - 1];
                     array[j - 1] = array[j];
                     array[j] = tem;
                }
            }
        }
    }
}
