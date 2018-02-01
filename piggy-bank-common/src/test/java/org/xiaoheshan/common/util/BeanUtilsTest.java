package org.xiaoheshan.common.util;

import org.junit.*;
import org.junit.rules.TestName;

/**
 * 测试结果
 *
 * @author _Chf
 * @since 01-31-2018
 */
public class BeanUtilsTest {

    private static SourcePojo sourcePojo = new SourcePojo();
    private static int pojoNum = 1000000;
    private long start = -1;
    private long end = -1;
    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.out.println("All test call times : " + pojoNum);
    }

    @Before
    public void testBefore() {
        start = System.currentTimeMillis();
    }

    @After
    public void testAfter() {
        end = System.currentTimeMillis();
        System.out.println(testName.getMethodName() + " cost: " + (end - start) + " ms");
    }

    @Test
    public void setterGetter() {
        for (int i = 0; i < pojoNum; i++) {
            TargetPojo targetPojo = new TargetPojo();
            targetPojo.setA(sourcePojo.getA());
            targetPojo.setB(sourcePojo.getB());
            targetPojo.setC(sourcePojo.getC());
            targetPojo.setD(sourcePojo.getD());
            Assert.assertTrue(targetPojo.getA() == 1);
        }
    }

    @Test
    public void beanUtils() {
        for (int i = 0; i < pojoNum; i++) {
            TargetPojo targetPojo = BeanUtils.instantiateAndCopy(TargetPojo.class, sourcePojo);
            Assert.assertTrue(targetPojo.getA() == 1);
        }
    }

    @Test
    public void beanCopier() {
        for (int i = 0; i < pojoNum; i++) {
            TargetPojo targetPojo = BeanCopier.instantiateAndCopy(TargetPojo.class, sourcePojo);
            Assert.assertTrue(targetPojo.getA() == 1);
        }
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
        private int a;
        private int b;
        private int c;
        private int d;
    }
}