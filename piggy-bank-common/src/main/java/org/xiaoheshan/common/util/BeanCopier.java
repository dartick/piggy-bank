package org.xiaoheshan.common.util;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 只支持名称及类型相同的转换，不支持简单类型及封装类型的转换
 *
 * @author _Chf
 * @since 02-01-2018
 */
public abstract class BeanCopier extends org.springframework.cglib.beans.BeanCopier {

    /**
     * 新建实例，并复制属性值
     *
     * @param source the source bean
     * @param clazz the target bean class
     * @param <T> target type
     * @return the new instance
     */
    public static <T> T instantiateAndCopy(Class<T> clazz, Object source) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(source, "source must not be null");
        T target = BeanUtils.instantiate(clazz);
        create(source.getClass(), clazz, false).copy(source, target, null);
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
        Collection result = BeanUtils.instantiate(collectionClazz);
        if (source.getClass().isArray()) {
            int length = Array.getLength(source);
            for (int i = 0; i < length; i++) {
                T target = instantiateAndCopy(targetClazz, Array.get(source, i));
                result.add(target);
            }
        }
        else if (source instanceof Collection) {
            for (Object o : ((Collection) source)) {
                T target = instantiateAndCopy(targetClazz, o);
                result.add(target);
            }
        }
        else {
            T target = instantiateAndCopy(targetClazz, source);
            result.add(target);
        }
        return result;
    }
}
