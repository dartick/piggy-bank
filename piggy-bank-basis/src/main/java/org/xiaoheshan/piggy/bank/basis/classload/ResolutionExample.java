package org.xiaoheshan.piggy.bank.basis.classload;

/**
 * @author _Chf
 * @since 08-18-2019
 */
public class ResolutionExample {

    /**
     * 验证使用其他类的class对象时, 是否会解析其方法及字段
     *
     * 验证结果: 方法及字段已解析
     */
    public void useOtherClass() {
        Class<ExampleClass> exampleClassClass = ExampleClass.class;
        System.out.println(exampleClassClass);
    }

    public void useOtherClassInstance() {
        ExampleClass exampleClass = new ExampleClass();
        System.out.println(exampleClass);
    }

    public void useOtherClassField() {
        ExampleClass exampleClass = new ExampleClass();
        int a = exampleClass.a;
        System.out.println(a);
    }

    public static void main(String[] args) {
        ResolutionExample resolutionExample = new ResolutionExample();
        resolutionExample.useOtherClass();
//        resolutionExample.useOtherClassField();
//        resolutionExample.useOtherClassInstance();
    }

    public static class ExampleClass {

        public static int s = 1;

        public int a = 0;

        static {
            System.out.println("I'm coming !!!");
        }

        public void fun1() {
            System.out.println("fun1 executing !!!");
        }
    }


}
