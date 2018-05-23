package org.xiaoheshan.piggy.bank.basis.serialization;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;

/**
 * @author _Chf
 * @since 05-21-2018
 */

public class SerializableTest {

    @Test
    public void testNormal() throws Exception {

        TestObject testObject = new TestObject();
        testObject.testValue = 100;
        testObject.parentValue = 101;
        testObject.innerObject = new InnerObject();
        testObject.innerObject.innerValue = 200;

        FileOutputStream fos = new FileOutputStream("temp.out");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(testObject);
        oos.flush();
        oos.close();

        FileInputStream fis = new FileInputStream("temp.out");
        ObjectInputStream ois = new ObjectInputStream(fis);
        TestObject deTest = (TestObject) ois.readObject();

        Assert.assertNotNull(deTest);
        Assert.assertEquals(testObject.testValue, deTest.testValue);
        Assert.assertEquals(testObject.parentValue, deTest.parentValue);
        Assert.assertNotNull(testObject.innerObject);
        Assert.assertEquals(testObject.innerObject.innerValue, deTest.innerObject.innerValue);
    }
}

class Parent implements Serializable {

    private static final long serialVersionUID = -4963266899668807475L;

    public int parentValue;
}

class TestObject extends Parent implements Serializable {

    private static final long serialVersionUID = -3186721026267206914L;

    public int testValue;

    public InnerObject innerObject;
}

class InnerObject implements Serializable {


    private static final long serialVersionUID = 5704957411985783570L;
    public int innerValue;

}
