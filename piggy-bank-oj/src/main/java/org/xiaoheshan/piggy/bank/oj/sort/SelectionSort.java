package org.xiaoheshan.piggy.bank.oj.sort;

public class SelectionSort {


    public static void sort(int[] array) {
        if (array == null || array.length == 0) {
            return;
        }
        for (int i = 0; i < array.length; i++) {
            int minIndex = i;
            for (int j = i + 1; j < array.length; j++) {
                if (array[j] < array[minIndex]) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                int tem = array[minIndex];
                array[minIndex] = array[i];
                array[i] = tem;
            }
        }
    }
}
