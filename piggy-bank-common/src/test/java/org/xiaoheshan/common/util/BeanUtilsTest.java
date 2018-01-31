package org.xiaoheshan.common.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 测试结果
 *
 * @author _Chf
 * @since 01-31-2018
 */
public class BeanUtilsTest {

    private SourcePojo sourcePojo = new SourcePojo();
    private int pojoNum = 100000;

    @Test
    public void testSetterGetter() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < pojoNum; i++) {
            TargetPojo targetPojo = new TargetPojo();
            targetPojo.setA(sourcePojo.getA());
            targetPojo.setB(sourcePojo.getB());
            targetPojo.setC(sourcePojo.getC());
            targetPojo.setD(sourcePojo.getD());
        }
        long end = System.currentTimeMillis();
        System.out.println("setter/getter used: " + (end - start) + " ms");
    }

    @Test
    public void testCopyProperties() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < pojoNum; i++) {
            BeanUtils.instantiateAndCopy(TargetPojo.class, sourcePojo);
        }
        long end = System.currentTimeMillis();
        System.out.println("BeanUtils used: " + (end - start) + " ms");
    }


    @lombok.Data
    public static class SourcePojo {
        private Integer a = 1;
        private Integer b = 1;
        private Integer c = 1;
        private Integer d = 1;
    }

    @lombok.Data
    public static class TargetPojo {
        private Integer a;
        private Integer b;
        private Integer c;
        private Integer d;
    }
}