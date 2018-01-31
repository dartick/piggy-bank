package org.xiaoheshan.common.util;

import org.springframework.util.Assert;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author _Chf
 * @since 01-31-2018
 */
public abstract class BeanUtils extends org.springframework.beans.BeanUtils {

    /**
     * 新建实例，并复制属性值
     *
     * @param source the source bean
     * @param clazz the target bean class
     * @param <T> target type
     * @return the new instance
     */
    public static <T> T instantiateAndCopy(Class<T> clazz, Object source) {
        T target = instantiate(clazz);
        copyProperties(source, target);
        return target;
    }

    /**
     * 容器属性复制，返回{@link List}实例
     *
     * @param source the source bean
     * @param clazz the target bean class
     * @param <T> target type
     * @return the new list instance
     */
    public static <T> List<T> copyToList(Object source, Class<T> clazz) {
        return (List<T>) copyToCollection(source, clazz, ArrayList.class);
    }

    /**
     * 容器属性复制，返回{@link Collection}实例
     *
     * @param source the source bean
     * @param targetClazz the target bean class
     * @param collectionClazz the collection clazz
     * @param <T> target type
     * @return the new collection instance
     */
    @SuppressWarnings("unchecked")
    public static <T> Collection<T> copyToCollection(Object source,
                                                     Class<T> targetClazz,
                                                     Class<? extends Collection> collectionClazz) {
        Assert.notNull(source, "source must not be null");
        Assert.notNull(targetClazz, "TargetType must not be null");
        Assert.notNull(targetClazz, "CollectionType must not be null");
        Collection result = instantiate(collectionClazz);
        if (source.getClass().isArray()) {
            int length = Array.getLength(source);
            for (int i = 0; i < length; i++) {
                T target = instantiate(targetClazz);
                copyProperties(Array.get(source, i), target);
                result.add(target);
            }
        }
        else if (source instanceof Collection) {
            for (Object o : ((Collection) source)) {
                T target = instantiate(targetClazz);
                copyProperties(o, target);
                result.add(target);
            }
        }
        else {
            T target = instantiate(targetClazz);
            copyProperties(source, target);
            result.add(target);
        }
        return result;
    }

}
