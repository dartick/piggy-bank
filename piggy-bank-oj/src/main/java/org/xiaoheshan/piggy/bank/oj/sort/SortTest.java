package org.xiaoheshan.piggy.bank.oj.sort;

import java.util.Arrays;

public class SortTest {


    public static void main(String[] args) {
        int[] array = new int[] {8,7,6,9,1,4,2};

//        BubbleSort.sort(array);
//        SelectionSort.sort(array);
//        InsertionSort.sort(array);
//        QuickSort.sort(array);
        MergeSort.sort(array);
        System.out.println(Arrays.toString(array));
    }
}
