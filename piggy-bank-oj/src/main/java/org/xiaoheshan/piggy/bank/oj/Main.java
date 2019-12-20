package org.xiaoheshan.piggy.bank.oj;

import java.util.*;

public class Main {


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int nums = scanner.nextInt();
        Map<Integer, Integer> indexMap = new TreeMap<>();
        for (int i = 0; i < nums; i++) {
            int index = scanner.nextInt();
            int value = scanner.nextInt();
            Integer sum = indexMap.getOrDefault(index, 0);
            indexMap.put(index, sum + value);
        }
        for (Map.Entry<Integer, Integer> entry : indexMap.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }
}
